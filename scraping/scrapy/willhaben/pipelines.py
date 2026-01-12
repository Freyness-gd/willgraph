# Define your item pipelines here
#
# Don't forget to add your pipeline to the ITEM_PIPELINES setting
# See: https://docs.scrapy.org/en/latest/topics/item-pipeline.html


# useful for handling different item types with a single interface
import re
from datetime import datetime
from typing import Any, Dict, List, Optional
from itemadapter import ItemAdapter
from scrapy.exceptions import DropItem, NotConfigured

try:
    from neo4j import GraphDatabase
except ImportError:  # pragma: no cover - optional dependency
    GraphDatabase = None

from willhaben.items import ImmoscoutItem, WillhabenItem
from willhaben.scriptToParseOsmId import query_location

class WillhabenPipeline:
    def process_item(self, item, spider):
        if not isinstance(item, WillhabenItem):
            return item

        adapter = ItemAdapter(item)

        adapter['size_m2'] = self._to_float(adapter.get('raw_size'))
        adapter['rooms'] = self._to_int(adapter.get('rooms'))

        price_raw = adapter.get('price_raw')

        adapter['price_eur'] = self._parse_price(price_raw)
        #remove the Bezirk from the current location

        adapter['location'] = self._parse_location(adapter.get('location'))

        osmLocation = query_location(adapter['location'])

        adapter['osm_id'] = osmLocation.get('osm_id')
        adapter['lon'] = osmLocation.get('lon')
        adapter['lat'] = osmLocation.get('lat')

        return item
    
    def _to_float(self, value):
        if value is None:
            return None
        if isinstance(value, (int, float)):
            return float(value)

        text = str(value).strip()
        if not text:
            return None
        # remove thousand separators, handle European decimal
        text = text.replace('.', '').replace(' ', '')
        text = text.replace(',', '.')

        try:
            return float(text)
        except ValueError:
            return None
        

    def _to_int(self, value):
        if value is None:
            return None
        if isinstance(value, int):
            return value
        text = str(value).strip()
        if not text:
            return None
        try:
            return int(float(text))
        except ValueError:
            return None

    def _parse_price(self, value):
        if value is None:
            return None
        text = str(value).strip()
        # keep only digits, dots and commas
        text = re.sub(r'[^\d\.,]', '', text)
        # remove thousand separators (dots/spaces), keep comma as decimal
        text = text.replace('.', '').replace(' ', '')

        # change comma to dot for decimal
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
        value = str(value).strip()
        value = value.replace('"', '')
        value = re.sub(r"\d+\. Bezirk,\s*", "", value)

        if value == '':
            return None
        return value
    

class ImmoscoutPipeline:
    async def process_item(self, item, spider):
        if not isinstance(item, ImmoscoutItem):
            return item

        adapter = ItemAdapter(item)

        adapter['size_m2'] = self._to_float(adapter.get('raw_size'))
        adapter['rooms'] = self._to_float_rooms(adapter.get('raw_rooms'))
        price_raw = adapter.get('price_raw')
        adapter['price_eur'] = self._parse_price(price_raw)
        #removes the Bezirk name from the location
        adapter['location'] = self._parse_location(adapter.get('location'))

        osmLocation = query_location(adapter['location'])

        adapter['osm_id'] = osmLocation.get('osm_id')
        adapter['lon'] = osmLocation.get('lon')
        adapter['lat'] = osmLocation.get('lat')

        return item
    
    def _to_float(self, value):
        if value is None:
            return None
        if isinstance(value, (int, float)):
            return float(value)

        text = str(value).strip()
        if not text:
            return None
        # remove thousand separators, handle European decimal
        text = text.replace(' m²', '')
        text = text.replace('.', '').replace(' ', '')
        text = text.replace(',', '.')

        try:
            return float(text)
        except ValueError:
            return None
        

    def _to_float_rooms(self, value):
        if value is None:
            return None
        if isinstance(value, float):
            return value
        text = str(value).strip()
        text = text.replace(' Zimmer', '')
        if not text:
            return None
        try:
            return float(text)
        except ValueError:
            return None

    def _parse_price(self, value):
        if value is None:
            return None
        text = str(value).strip()
        # keep only digits, dots and commas -- removes currency symbol and spaces
        text = re.sub(r'[^\d\.,]', '', text)
        # remove thousand separators (dots/spaces), keep comma as decimal
        text = text.replace('.', '').replace(' ', '')

        # change comma to dot for decimal
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
        value = str(value).strip()
        value = value.replace('"', '')
        #remove everything after Wien, 
        value = re.sub(r"Wien.*", "Wien", value)

        if value == '':
            return None
        return value

class Neo4jPipeline:
    """Writes listing items to Neo4j using parameterized Cypher with batching."""

    CYPHER_UPSERT = """
        UNWIND $rows AS row

        MERGE (re:`Real Estate` {url: row.url})
        SET
            re.livingArea = coalesce(row.size_m2, re.livingArea),
            re.area       = coalesce(row.size_m2, re.area),
            re.found      = coalesce(row.scraped_at, re.found),
            re.source     = coalesce(row.source, re.source)

        MERGE (rent:Rental {url: row.url})
        SET
            rent.priceInEur = coalesce(row.price_eur, rent.priceInEur),
            rent.priceAt    = coalesce(row.scraped_at, rent.priceAt)

        MERGE (rm:Rooms {url: row.url})
        SET
            rm.count = coalesce(row.rooms, rm.count)

        MERGE (addr:Address {streetId: row.osm_id})
        SET
            addr.Street    = coalesce(row.location, addr.Street),
            addr.latitude  = coalesce(row.lat, addr.latitude),
            addr.longitude = coalesce(row.lon, addr.longitude)

        MERGE (re)-[:HAS_ROOMS]->(rm)
        MERGE (re)-[:HAS_TYPE]->(rent)
        MERGE (re)-[:IS_IN]->(addr)
        MERGE (addr)-[:HAS_REAL_ESTATE]->(re)
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
        batch_size = settings.getint("NEO4J_BATCH_SIZE", 200)
        return cls(uri, user, password, database, batch_size)

    def open_spider(self, spider):
        self.driver = GraphDatabase.driver(self.uri, auth=(self.user, self.password))
        self.session = self.driver.session(database=self.database)

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
            "size_m2": _val("size_m2"),
            "price_eur": _val("price_eur"),
            "rooms": _val("rooms"),
            "location": _val("location"),
            "osm_id": _val("osm_id"),
            "lat": _val("lat"),
            "lon": _val("lon"),
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