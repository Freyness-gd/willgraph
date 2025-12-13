# Define your item pipelines here
#
# Don't forget to add your pipeline to the ITEM_PIPELINES setting
# See: https://docs.scrapy.org/en/latest/topics/item-pipeline.html


# useful for handling different item types with a single interface
import re
from itemadapter import ItemAdapter
from scrapy_files.items import WillhabenItem
from scrapy_files.scriptToParseOsmId import  query_location
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