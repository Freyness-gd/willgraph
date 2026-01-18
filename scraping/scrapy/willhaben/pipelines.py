# Define your item pipelines here
#
# Don't forget to add your pipeline to the ITEM_PIPELINES setting
# See: https://docs.scrapy.org/en/latest/topics/item-pipeline.html


import hashlib
import re
from datetime import datetime
from typing import Any, Dict, Iterable, List, Optional, Sequence, Tuple
from itemadapter import ItemAdapter
from scrapy.exceptions import DropItem, NotConfigured
import math
try:
    from neo4j import GraphDatabase
except ImportError:  # pragma: no cover - optional dependency
    GraphDatabase = None

from willhaben.items import ImmoscoutItem, WillhabenItem
from willhaben.scriptToParseOsmId import query_location
MIN_PRICE_EUR = 100
MAX_PRICE_EUR = 100_000
MIN_SIZE_M2 = 5
MAX_SIZE_M2 = 1000

class ValidationPipeline:
    """Validates items against required fields, bounds, and quality checks."""

    # Reasonable bounds for real estate listings

    MIN_ROOMS = 0.5
    MAX_ROOMS = 20

    def __init__(self):
        self.stats = {
            'validated': 0,
            'dropped_missing_url': 0,
            'dropped_missing_title': 0,
            'dropped_missing_location': 0,
            'dropped_price_out_of_bounds': 0,
            'dropped_size_out_of_bounds': 0,
            'dropped_rooms_out_of_bounds': 0,
        }

    def process_item(self, item, spider):
        if not isinstance(item, (ImmoscoutItem, WillhabenItem)):
            return item

        adapter = ItemAdapter(item)
        reason = self._validate(adapter)

        if reason:
            spider.logger.warning(
                f"ValidationPipeline dropped item from {spider.name}: {reason} | url={adapter.get('url')}"
            )
            self.stats[reason] += 1
            raise DropItem(reason)

        self.stats['validated'] += 1
        return item

    def _validate(self, adapter: ItemAdapter) -> Optional[str]:
        """Return validation failure reason or None if valid."""
        url = adapter.get('url')
        title = adapter.get('title')
        location = adapter.get('location')
        price = adapter.get('price_eur')
        size = adapter.get('size_m2')
        rooms = adapter.get('rooms')

        # Required fields
        if not url or not str(url).strip():
            return 'dropped_missing_url'
        if not title or not str(title).strip():
            return 'dropped_missing_title'
        if not location or not str(location).strip():
            return 'dropped_missing_location'
        
        if price is  None:
            return 'dropped_price_none'
        if size is None:
            return 'dropped_size_none'
        if rooms is None:
            return 'dropped_rooms_none'
        
        # Normalized numeric bounds (will anyways only be validated if not none)
        if price is not None and not (MIN_PRICE_EUR <= price <= MAX_PRICE_EUR):
            return 'dropped_price_out_of_bounds'

        if size is not None and not  (MIN_SIZE_M2 <= size <= MAX_SIZE_M2):
            return 'dropped_size_out_of_bounds'

        if rooms is not None and not (self.MIN_ROOMS <= rooms <= self.MAX_ROOMS):
            return 'dropped_rooms_out_of_bounds'

        return None

    def close_spider(self, spider):
        spider.logger.info(f"ValidationPipeline stats: {self.stats}")


class DeduplicationPipeline:
    """Deduplicates items based on content similarity with price/size tolerance."""

    # Tolerance ranges for considering items similar
    PRICE_TOLERANCE_PCT = 1  # 1% price difference allowed
    SIZE_TOLERANCE_PCT = 0.5   # 0.5% size difference allowed

    def __init__(self):
        self.seen_hashes: set = set()
        self.stats = {
            'processed': 0,
            'dropped_duplicate': 0,
        }

    def process_item(self, item, spider):
        if not isinstance(item, (ImmoscoutItem, WillhabenItem)):
            return item

        adapter = ItemAdapter(item)
        item_hash = self._compute_hash(adapter)

        if item_hash in self.seen_hashes:
            spider.logger.debug(
                f"DeduplicationPipeline dropped duplicate: {adapter.get('url')}"
            )
            self.stats['dropped_duplicate'] += 1
            raise DropItem(f"Duplicate item (hash: {item_hash})")

        self.seen_hashes.add(item_hash)
        self.stats['processed'] += 1
        return item

    def _compute_hash(self, adapter: ItemAdapter) -> str:
        """Create a hash based on location, title, price, and size with tolerance."""
        location = adapter.get('location', '')
        title = adapter.get('title', '')
        price = adapter.get('price_eur')
        size = adapter.get('size_m2')

        #this will at least work on listings with the same title

        # Quantize price and size to tolerance ranges to catch near-duplicates
        price_bucket = self._quantize(price, MIN_PRICE_EUR, MAX_PRICE_EUR, self.PRICE_TOLERANCE_PCT) if price else 'None'
        size_bucket = self._quantize(size, MIN_SIZE_M2, MAX_SIZE_M2, self.SIZE_TOLERANCE_PCT) if size else 'None'

        # Create signature from normalized fields (excluding URL which is unique)
        signature = f"{location}|{title}|{price_bucket}|{size_bucket}".lower()
        return hashlib.sha256(signature.encode()).hexdigest()

    @staticmethod
    def _quantize(value: float, min_v: float, max_v: float, pct: float) -> int:
        """Quantize value into buckets of tolerance_pct for near-duplicate detection."""

        if value is None:
            return -1
        if pct <= 0:
            raise ValueError("pct must be > 0")
        if max_v <= min_v:
            raise ValueError("max_v must be > min_v")

        span = max_v - min_v
        bucket_size = span * (pct / 100.0)
        n_buckets = int(math.ceil(span / bucket_size))  # for pct=5 -> 20

        # clamp to range
        v = min(max(value, min_v), max_v)

        # map to bucket; ensure max_v lands in last bucket
        idx = int((v - min_v) // bucket_size)
        return min(idx, n_buckets - 1)

    def close_spider(self, spider):
        spider.logger.info(f"DeduplicationPipeline stats: {self.stats}")


class BaseCleaningPipeline:
    """Reusable parsing/normalization helpers for heterogeneous sources."""

    location_regex_subs: Sequence[Tuple[str, str]] = ()

    def __init__(self):
        self.osm_failures = 0

    def _normalize_common(self, adapter: ItemAdapter, spider=None) -> None:
        adapter['size_m2'] = self._to_float(adapter.get('raw_size'), drop_tokens=('m²', 'm2', ' m²'))
        adapter['rooms'] = self._to_float(adapter.get('raw_rooms'), drop_tokens=('Zimmer',))
        adapter['price_eur'] = self._parse_price(adapter.get('price_raw'))
        if(adapter.get('osm_id') is None or adapter.get('lat') is None or adapter.get('lon') is None):
            adapter['location'] = self._parse_location(adapter.get('location'))
            self._attach_osm(adapter, spider)

    def _to_float(self, value, drop_tokens: Iterable[str] = ()):  
        if value is None:
            return None
        if isinstance(value, (int, float)):
            return float(value)

        text = str(value).strip()
        if not text:
            return None
        for token in drop_tokens:
            text = text.replace(token, '')
        text = text.replace('.', '').replace(' ', '')
        text = text.replace(',', '.')
        try:
            return float(text)
        except ValueError:
            return None

    def _parse_price(self, value):  
        if value is None:
            return None
        text = str(value).strip()
        text = re.sub(r'[^\d\.,]', '', text)
        text = text.replace('.', '').replace(' ', '')
        text = text.replace(',', '.')
        if not text:
            return None
        try:
            return float(text)
        except ValueError:
            return None

    def _parse_location(self, value):  
        if value is None:
            return None
        text = str(value).strip().replace('"', '')
        for pattern, repl in self.location_regex_subs:
            text = re.sub(pattern, repl, text)
        text = text.strip()
        return text or None

    def _attach_osm(self, adapter: ItemAdapter, spider=None) -> None:
        location = adapter.get('location')
        if not location:
            return
        if(not adapter.get('osm_id') or not adapter.get('lat') or not adapter.get('lon')):
            try:
                osm_location = query_location(location)
                adapter['osm_id'] = osm_location.get('osm_id')
                adapter['lon'] = osm_location.get('lon')
                adapter['lat'] = osm_location.get('lat')
            except Exception as e:
                self.osm_failures += 1
                if spider:
                    spider.logger.warning(f"OSM lookup failed for location '{location}': {e}")
                    raise DropItem(f"OSM lookup failed for location '{location}': {e}")
                else:
                    import logging
                    logging.getLogger(__name__).warning(f"OSM lookup failed for location '{location}': {e}")
                    raise DropItem(f"OSM lookup failed for location '{location}': {e}")


    def close_spider(self, spider):
        if self.osm_failures > 0:
            spider.logger.info(f"BaseCleaningPipeline: {self.osm_failures} OSM lookup failures")


class WillhabenPipeline(BaseCleaningPipeline):
    location_regex_subs = (
        (r"\d+\. Bezirk,\s*", ""),
        (r"\b(Top|Tür|Stiege)\s+\d+\b", ""),
        (r"\bNähe\b", "") #this may be overfitting
    )

    def process_item(self, item, spider):  
        if not isinstance(item, WillhabenItem):
            return item

        adapter = ItemAdapter(item)
        self._normalize_common(adapter, spider)
        return item


class ImmoscoutPipeline(BaseCleaningPipeline):
    location_regex_subs = (
        (r"Wien.*", "Wien"),
    )

    async def process_item(self, item, spider):  
        if not isinstance(item, ImmoscoutItem):
            return item

        adapter = ItemAdapter(item)
        
        # Clean title: replace newlines, carriage returns, and tabs with two spaces
        title = adapter.get('title')
        if title:
            cleaned_title = re.sub(r'[\r\n\t]+', '  ', str(title))
            cleaned_title = re.sub(r' {3,}', '  ', cleaned_title)
            cleaned_title = cleaned_title.strip()
            adapter['title'] = cleaned_title
        
        self._normalize_common(adapter)
        return item

class Neo4jPipeline:
    """Writes listing items to Neo4j using parameterized Cypher with batching."""

    CYPHER_UPSERT = """
        UNWIND $rows AS row
        WITH row WHERE row.url IS NOT NULL

        // Upsert Listing node (matches Spring ListingImporter / ListingEntity)
        MERGE (l:Listing {url: row.url})
        SET
            l.title          = coalesce(row.title, l.title),
            l.price          = coalesce(row.price_eur, l.price),
            l.livingArea     = coalesce(row.size_m2, l.livingArea),
            l.roomCount      = coalesce(row.rooms, l.roomCount),
            l.source         = coalesce(row.source, l.source),
            l.timestampFound = coalesce(row.scraped_at, l.timestampFound)

        // Resolve / create Address node
        WITH l, row
        CALL {
        WITH row
            MERGE (a:Address {osmId: row.osm_id})
            SET a.fullAddressString = coalesce(row.location, a.fullAddressString),
                a.location = coalesce(
                    CASE
                    WHEN row.lat IS NOT NULL AND row.lon IS NOT NULL
                        THEN point({latitude: row.lat, longitude: row.lon})
                    END,
                    a.location)
            RETURN a
        }

        // Connect Listing -> Address if an address exists
        WITH l, a, row WHERE a IS NOT NULL
        MERGE (l)-[:LOCATED_AT]->(a)
        """

    def __init__(self, uri: str, user: str, password: str, database: Optional[str] = None, batch_size: int = 200):
        if GraphDatabase is None:
            raise NotConfigured("neo4j driver not installed; add 'neo4j' to requirements")

        if not (uri and user and password):
            raise NotConfigured("NEO4J_URI, NEO4J_USER, NEO4J_PASSWORD must be set")

        self.uri = uri
        self.user = user
        self.password = password
        self.database = database
        self.driver = None
        self.session = None
        self.batch: List[Dict[str, Any]] = []
        self.batch_size = max(1, batch_size)

    @classmethod
    def from_crawler(cls, crawler):
        settings = crawler.settings
        uri = settings.get("NEO4J_URI")
        user = settings.get("NEO4J_USER")
        password = settings.get("NEO4J_PASSWORD")
        database = settings.get("NEO4J_DATABASE")
        batch_size = settings.getint("NEO4J_BATCH_SIZE", 10)
        return cls(uri, user, password, database, batch_size)

    def open_spider(self, spider):
        self.driver = GraphDatabase.driver(self.uri, auth=(self.user, self.password))
        self.session = self.driver.session()

    def close_spider(self, spider)  :
        self._flush_batch()
        if self.session:
            self.session.close()
        if self.driver:
            self.driver.close()

    def process_item(self, item, spider):
        if not isinstance(item, (ImmoscoutItem, WillhabenItem)):
            return item

        adapter = ItemAdapter(item)
        url = adapter.get("url")
        if not url:
            spider.logger.warning("Neo4jPipeline: missing url, skipping item")
            raise DropItem("missing url")

        def _val(key: str):
            v = adapter.get(key)
            return v.isoformat() if isinstance(v, datetime) else v

        self.batch.append({
            "url": url,
            "title": _val("title"),
            "size_m2": _val("size_m2"),
            "price_eur": _val("price_eur"),
            "rooms": _val("rooms"),
            "location": _val("location"),
            "osm_id": _val("osm_id"),
            "lat": float(adapter.get("lat")) if adapter.get("lat") is not None else None,
            "lon": float(adapter.get("lon")) if adapter.get("lon") is not None else None,
            "scraped_at": _val("scraped_at"),
            "source": spider.name,
        })
        if len(self.batch) >= self.batch_size:
            self._flush_batch()

        return item

    def _flush_batch(self):
        print("flushing batch to Neo4j..." + str(len(self.batch)))
        if not self.batch or self.session is None:
            return

        rows = self.batch
        self.batch = []

        def _txn(tx, data):
            tx.run(self.CYPHER_UPSERT, rows=data)

        self.session.execute_write(_txn, rows)