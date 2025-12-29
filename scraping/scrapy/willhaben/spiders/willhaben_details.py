import json

import scrapy


class WillhabenDetailsSpider(scrapy.Spider):
    name = "willhaben_details"
    #scrapes the lon and lat from the json blocks to get an actual location
    start_urls = [
        "https://www.willhaben.at/iad/immobilien/d/mietwohnungen/wien/wien-1020-leopoldstadt/hofruhelage-top-singlewohnung-1561645130/"
    ]

    # this can be used to scrape exact lon and lat
    # this can also be used to scrape the whole details here instead of the listings
    #populate list of urls by fetching them from the listing page like this
    #for block in response.xpath('//script[@type="application/ld+json"]/text()').getall():
    #    data = json.loads(block)
    #    if isinstance(data, dict) and data.get("@type") == "ItemList":
    #        for item in data["itemListElement"]:
    #            yield response.urljoin(item["url"])
    # then one can also use reverse geocoding of nominatim to actually fetch the osm_id


    def parse(self, response):
        # Select all JSON-LD script tags
        json_ld_blocks = response.xpath('//script[@type="application/ld+json"]/text()').getall()

        for block in json_ld_blocks:
            try:
                data = json.loads(block)
                # Some ld+json blocks are lists; normalize them
                if isinstance(data, list):
                    items = data
                else:
                    items = [data]

                # Search for geo coordinates
                for item in items:
                    geo = item.get("offers", {}).get("availableAtOrFrom", {}).get("geo", {})
                    if geo:
                        yield {
                            "latitude": geo.get("latitude"),
                            "longitude": geo.get("longitude"),
                        }

            except json.JSONDecodeError:
                pass