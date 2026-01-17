import time
import re
import requests
import urllib.parse

import scrapy
import datetime
from willhaben.items import WillhabenItem
from scrapy_playwright.page import PageMethod

class WillhabenSpider(scrapy.Spider):
    name = "willhaben"
    allowed_domains=["willhaben.at"]
    custom_settings = {
        'FEEDS': {
            'willhaben_output.csv': {
                'format': 'csv',
                'encoding': 'utf8',
                'overwrite': True,
            },
        },
        "ITEM_PIPELINES": {
            "willhaben.pipelines.WillhabenPipeline": 300,
            "willhaben.pipelines.ValidationPipeline": 350,  # runs after cleaning
            "willhaben.pipelines.DeduplicationPipeline": 375,  # runs after cleaning
            "willhaben.pipelines.Neo4jPipeline": 400, #first 300 is done then 400 as 400 > 300

        }
    }

    def __init__(self, *args, **kwargs):
        super(WillhabenSpider, self).__init__(*args, **kwargs)
        self.nominatim_cache = {}  # Cache for Nominatim requests keyed by (lat, lon)
        self.last_nominatim_request_time = 0  # Track last Nominatim request time for throttling

    slow_scroll = """
                        () => new Promise(resolve => {
                            const distance = 600;
                            const delay = 400;
                            const maxSteps = 50;
                            let steps = 0;

                            const timer = setInterval(() => {
                                window.scrollBy(0, distance);
                                steps += 1;

                                const atBottom = window.innerHeight + window.scrollY >= document.body.scrollHeight;
                                if (atBottom || steps >= maxSteps) {
                                    clearInterval(timer);
                                    resolve();
                                }
                            }, delay);
                        })
                        """

    NOMINATIM_URL = "https://nominatim.openstreetmap.org/reverse"
    NOMINATIM_HEADERS = {
        "User-Agent": "WillhabenSpider/1.0"
    }

    def _is_simple_location(self, location):
        """Check if location matches format: 4-digit zip, Wien, Word"""
        # Remove Bezirk pattern (e.g., ", 06. Bezirk") before checking
        normalized_location = re.sub(r"\d+\. Bezirk,\s*", "", location)
        pattern = r'^\d{4}\s+Wien,\s+\w+$'
        return bool(re.match(pattern, normalized_location))

    def _reverse_geocode(self, lat, lon):
        """Use nominatim to reverse geocode coordinates to actual location"""
        try:
            params = {
                "lat": lat,
                "lon": lon,
                "format": "jsonv2",
                "email": "e12121286@student.tuwien.ac.at"
            }
            response = requests.get(self.NOMINATIM_URL, params=params, headers=self.NOMINATIM_HEADERS, timeout=10)
            if response.status_code == 200:
                data = response.json()
                return data.get("address", {}).get("address") or data.get("display_name")
            return None
        except Exception as e:
            self.logger.warning(f"Reverse geocoding failed for ({lat}, {lon}): {e}")
            return None

    def _query_nominatim_for_osm_id(self, lat, lon):
        """Query Nominatim with coordinates to get osm_id and location"""
        self.logger.info(f"Reverse geocoding ({lat}, {lon})")

        # Create cache key from coordinates
        cache_key = (str(lat), str(lon))
        
        # Check cache first
        if cache_key in self.nominatim_cache:
            self.logger.debug(f"Cache hit for coordinates ({lat}, {lon})")
            return self.nominatim_cache[cache_key]
        
        # Throttle requests to respect Nominatim's rate limit (1 request per second)
        time_since_last_request = time.time() - self.last_nominatim_request_time
        if time_since_last_request < 1.0:
            sleep_time = 1.0 - time_since_last_request
            self.logger.debug(f"Throttling Nominatim request, sleeping for {sleep_time:.2f}s")
            time.sleep(sleep_time)
        
        try:
            params = {
                "lat": lat,
                "lon": lon,
                "format": "jsonv2",
                "email": "e12121286@student.tuwien.ac.at"
            }
            response = requests.get(self.NOMINATIM_URL, params=params, headers=self.NOMINATIM_HEADERS, timeout=10)
            self.last_nominatim_request_time = time.time()
            
            if response.status_code == 200:
                data = response.json()
                result = {
                    "osm_id": data.get("osm_id"),
                    "location": data.get("address", {}).get("address") or data.get("display_name"),
                    "lat": lat,
                    "lon": lon
                }
                # Store in cache
                self.nominatim_cache[cache_key] = result
                return result
            return None
        except Exception as e:
            self.logger.warning(f"Nominatim query failed for ({lat}, {lon}): {e}")
            return None

    async def start(self):
        urls = [
            "https://www.willhaben.at/iad/immobilien/mietwohnungen/wien",
        ]
        for url in urls:
            yield scrapy.Request(url=url, callback=self.parse,
                meta={
                "playwright": True,
                "playwright_context": "extra",
                "playwright_page_methods": [
                    PageMethod(
                        "evaluate",
                        self.slow_scroll
                    ),
                    PageMethod("wait_for_timeout", 2000),
                ],
            })

    def parse(self, response):
        for dataEntry in response.css('a[id^="search-result-entry-header-"]'):
            #get href from search result div
            #in children there are multiple divs
            #we have one div with a preview image
            #i.e. if image then skip div
            #one div with an h3 and a sibling div that has svg with a span for location

            #one div with  a bunch of divs where one div will have a span with Zimmer one with m²
            #find siblings of the spans with zimmer and m² to get the values

            #one div with a span with price symbol
            url = response.urljoin(dataEntry.attrib.get("href"))

            title = dataEntry.css("h3::text").get(default="").strip()

            location = dataEntry.css(
                'span[aria-label^="Ort"]::text'
            ).get(default="").strip()

            size_text = dataEntry.xpath(
                './/span[contains(normalize-space(.), "m²")]/preceding-sibling::span[1]/text()'
            ).get()

            rooms_text = dataEntry.xpath(
                './/span[contains(normalize-space(.), "Zimmer")]/preceding-sibling::span[1]/text()'
            ).get()
            rooms = rooms_text.strip() if rooms_text else None

            price_raw = dataEntry.css(
                'span[data-testid^="search-result-entry-price-"]::text'
            ).get()
            price_raw = price_raw.strip() if price_raw else None
            scraped_at = datetime.datetime.now()

            # If location is simple format (zip Wien, district), fetch details for real location
            self.logger.info(f"Processing listing: {title} at {location}")
            self.logger.info(f"URL: {self._is_simple_location(location)}")
            if self._is_simple_location(location):
                yield scrapy.Request(
                    url=url,
                    callback=self.parse_details,
                    meta={
                        'item': {
                            'url': url,
                            'title': title,
                            'location': location,
                            'raw_size': size_text,
                            'raw_rooms': rooms,
                            'price_raw': price_raw,
                            'scraped_at': scraped_at
                        },
                    }
                )
            else:
                yield WillhabenItem(
                    url=url,
                    title=title,
                    location=location,
                    raw_size=size_text,
                    raw_rooms=rooms,
                    price_raw=price_raw,
                    scraped_at=scraped_at
                )

        next_button = response.css('a[aria-label^="Weiter"]::attr(href)').get()
        if next_button is not None:
            #remove sfid= from url if present
            #keep other other query params

            next_page = response.urljoin(next_button)
            yield scrapy.Request(url=next_page, callback=self.parse)

    def parse_details(self, response):
        """Parse details page to extract lat/lon and reverse geocode for actual location"""
        import json
        self.logger.info("Parsing details page for reverse geocoding")
        item = response.meta['item']
        lat = None
        lon = None
        osm_id = None
        actual_location = None
        
        # Extract coordinates from JSON-LD blocks
        json_ld_blocks = response.xpath('//script[@type="application/ld+json"]/text()').getall()
        
        for block in json_ld_blocks:
            try:
                data = json.loads(block)
                if isinstance(data, list):
                    items = data
                else:
                    items = [data]
                
                for json_item in items:
                    geo = json_item.get("offers", {}).get("availableAtOrFrom", {}).get("geo", {})
                    if geo:
                        lat = geo.get("latitude")
                        lon = geo.get("longitude")
                        break
                
                if lat and lon:
                    break
            except (json.JSONDecodeError, AttributeError):
                pass
        
        # If we found coordinates, reverse geocode to get actual location and OSM ID
        if lat and lon:
            osm_result = self._query_nominatim_for_osm_id(lat, lon)
            if osm_result:
                osm_id = osm_result.get('osm_id')
                actual_location = osm_result.get('location')
                if actual_location:
                    item['location'] = actual_location
                    self.logger.info(f"Reverse geocoded location: {item['location']} (OSM ID: {osm_id})")
        
        yield WillhabenItem(
            url=item['url'],
            title=item['title'],
            location=item['location'],
            raw_size=item['raw_size'],
            raw_rooms=item['raw_rooms'],
            price_raw=item['price_raw'],
            scraped_at=item['scraped_at'],
            osm_id=osm_id,
            lat=lat,
            lon=lon
        )
