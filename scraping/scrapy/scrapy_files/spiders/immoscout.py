import scrapy
import scrapy_playwright.handler
from scrapy_playwright.page import PageMethod


class ImmoscoutSpider(scrapy.Spider):
    name = "immoscout"
    handle_httpstatus_list = [401]
    async def start(self):
        url = "https://www.immobilienscout24.at/regional/oesterreich/wohnung-mieten"
        yield scrapy.Request(
            url,
            callback=self.solve_captcha,
            dont_filter=True,
            meta={
                "playwright": True,
                "playwright_include_page" : True,
                "playwright_context": "extra",
                "playwright_page_methods": [
                    PageMethod("pause"),  # <-- browser waits HERE
                    PageMethod("wait_for_load_state", "networkidle")
                ],
            },
        )

    async def solve_captcha(self, response):
        playwright_context = response.meta["playwright_page"]
        # After you finish solving the CAPTCHA in the real browser:
        # press the ▶ button in the Playwright Inspector to continue.
        # Now load the page again with the validated cookie
        return scrapy.Request(
            "https://www.immobilienscout24.at/regional/oesterreich/wohnung-mieten",
            callback=self.parse,
            dont_filter=True,
            meta={
                "playwright": True,
                "playwright_context": "extra",
            },
        )

    def parse(self, response):
        yield {"html": response.text}