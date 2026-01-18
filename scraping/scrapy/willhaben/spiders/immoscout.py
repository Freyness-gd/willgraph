from datetime import datetime
from willhaben.items import ImmoscoutItem
import scrapy
import scrapy_playwright.handler
from scrapy_playwright.page import PageMethod


class ImmoscoutSpider(scrapy.Spider):
    name = "immoscout"
    handle_httpstatus_list = [401]  # avoids crashes when we don't solve captcha
    custom_settings = {
        'FEEDS': {
            'immoscout_output.csv': {
                'format': 'csv',
                'encoding': 'utf8',
                'overwrite': True,
            },
        },
        "ITEM_PIPELINES": {
            "willhaben.pipelines.ImmoscoutPipeline": 300,
            "willhaben.pipelines.ValidationPipeline": 350,  # runs after cleaning
            "willhaben.pipelines.DeduplicationPipeline": 375,
            "willhaben.pipelines.Neo4jPipeline": 400,  # first 300 is done then 400 as 400 > 300
        },
        "PLAYWRIGHT_LAUNCH_OPTIONS": {
            "headless": False,
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
        url = "https://www.immobilienscout24.at/regional/oesterreich/wohnung-mieten"
        yield scrapy.Request(
            url,
            callback=self.solve_captcha,
            dont_filter=True,
            meta={
                "playwright": True,
                "playwright_include_page": True,
                "playwright_context": "extra",
                "playwright_page_methods": [
                    PageMethod("pause"),  # <-- browser waits HERE
                ],
            },
        )

    async def solve_captcha(self, response):
        playwright_context = response.meta["playwright_page"]
        # After you finish solving the CAPTCHA in the real browser:
        # press the ▶ button in the Playwright Inspector to continue.
        # Now load the page again with the validated cookie
        return scrapy.Request(
            "https://www.immobilienscout24.at/regional/wien/wien/wohnung-mieten",
            callback=self.parse,
            dont_filter=True,
            meta={
                "playwright": True,
                "playwright_context": "extra",
                "playwright_page_methods": [
                    PageMethod(
                        "evaluate",
                        self.slow_scroll
                    ),
                    PageMethod("wait_for_timeout", 5000),
                ],
            },
        )

    def parse_mega_listing(self, response):
        item = response.meta['item']
         # TODO get item from meta
        self.logger.info("Parsing mega listing page: %s", response.url)
        table = response.xpath(
            "//section[h3[normalize-space(.)='Mietwohnungen']]//div[contains(@class, 'ChildrenTable')]")
        listings = table.xpath("//div[contains(@role, 'button')]")
        listings_count = 0
        for listing in listings:
            area = listing.xpath(
                "normalize-space(string(.//span[contains(normalize-space(.), 'm²')]))"
            ).get()
            # Parent text of the span that contains "Zimmer"
            rooms = listing.xpath(
                "normalize-space(string(.//span[contains(normalize-space(.), 'Zimmer')]))"
            ).get()
            # Arbitrary-depth span containing € → get text of that span's parent
            price = listing.xpath(
                "normalize-space(string(.//span[contains(normalize-space(.), '€')]/parent::*[1]))"
            ).get()
            price = price.strip() if price else None
            area = area.strip() if area else None
            rooms = rooms.strip() if rooms else None
            scraped_at = datetime.now()
            yield ImmoscoutItem(
                mega_listing=True,
                url=item['url'] + "#" + str(listings_count),
                title=item['title'],
                location=item['location'],
                raw_size=area,
                raw_rooms=rooms,
                price_raw=price,
                scraped_at=scraped_at
            )
            listings_count += 1

    def parse(self, response):

        listings = response.css('ol[data-testid="results-items"] > li')

        self.logger.info(
            "ul_count=%s",
            response.xpath('count(//ul[@data-testid="results-properties-children"])').get()
        )

        for listing in listings:
            for link in listing.xpath('.//a[contains(@href,"/expose/")][.//h2][.//address]'):

                has_section = bool(link.css('section').get())
                #

                children_ul = listing.xpath('.//ul[@data-testid="results-properties-children"]')
                children_items = listing.xpath('.//li[@data-testid="results-properties-children-item"]')
                children_expose_links = listing.xpath('.//ul//a[contains(@href,"/expose/")]')

                has_children = (
                        bool(children_ul.get())
                        or bool(children_items.get())
                        or len(children_expose_links) > 1
                )

                # if listing has a div with ul then make has ul true this means its a listing for a building with multiple apartments
                url = response.urljoin(link.attrib.get("href"))
                address = link.xpath('.//address[contains(@class,"Address")]/text()').get()
                address = address.strip() if address else None
                
                title = link.xpath('.//h2/text()').get()
                title = title.strip() if title else None

                if has_section and not has_children:

                    price = link.xpath('.//ul[contains(@class,"PriceKeyFacts")]/li[1]//text()').get()
                    price = price.strip() if price else None

                    area = link.xpath(
                        './/ul[contains(@class,"KeyFacts") and not(contains(@class,"PriceKeyFacts"))]/li[contains(normalize-space(.),"m²")]/text()'
                    ).get()
                    area = area.strip() if area else None

                    rooms = link.xpath(
                        './/ul[contains(@class,"KeyFacts") and not(contains(@class,"PriceKeyFacts"))]/li[contains(normalize-space(.),"Zimmer")]/text()'
                    ).get()
                    rooms = rooms.strip() if rooms else None


                    scraped_at = datetime.now()
                    yield ImmoscoutItem(
                        mega_listing=has_children,
                        url=url,
                        title=title,
                        location=address,
                        raw_size=area,
                        raw_rooms=rooms,
                        price_raw=price,
                        scraped_at=scraped_at
                    )
                elif has_children:
                    yield scrapy.Request(
                        url=response.urljoin(link.attrib.get("href")),
                        callback=self.parse_mega_listing,
                        meta={
                            'item': {
                            'url': url,
                            'title': title,
                            'location': address,
                            },
                            "playwright": True,
                            "playwright_context": "extra",
                            "playwright_page_methods": [
                                PageMethod(
                                    "evaluate",
                                    self.slow_scroll
                                ),
                                PageMethod("wait_for_timeout", 5000),
                            ],
                        },
                    )

        next_button = response.css('a[aria-label="weiter"]::attr(href)').get()
        self.logger.info(
            "ul_count=%s",
            next_button
        )
        if next_button is not None:
            next_page = response.urljoin(next_button)
            yield scrapy.Request(url=next_page, callback=self.parse, meta={
                "playwright": True,
                "playwright_context": "extra",
                "playwright_page_methods": [
                    PageMethod(
                        "evaluate",
                        self.slow_scroll
                    ),
                    PageMethod("wait_for_timeout", 5000),
                ],
            }, )