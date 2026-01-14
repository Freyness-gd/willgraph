import time

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
            #"willhaben.pipelines.Neo4jPipeline": 400, #first 300 is done then 400 as 400 > 300

        }
    }

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


