# Define here the models for your spider middleware
#
# See documentation in:
# https://docs.scrapy.org/en/latest/topics/spider-middleware.html

from scrapy import signals
from scrapy.exceptions import CloseSpider
import random
# useful for handling different item types with a single interface
from itemadapter import ItemAdapter


class WillhabenSpiderMiddleware:
    # Not all methods need to be defined. If a method is not defined,
    # scrapy acts as if the spider middleware does not modify the
    # passed objects.

    @classmethod
    def from_crawler(cls, crawler):
        # This method is used by Scrapy to create your spiders.
        s = cls()
        crawler.signals.connect(s.spider_opened, signal=signals.spider_opened)
        return s

    def process_spider_input(self, response, spider):
        # Called for each response that goes through the spider
        # middleware and into the spider.

        # Should return None or raise an exception.
        return None

    def process_spider_output(self, response, result, spider):
        # Called with the results returned from the Spider, after
        # it has processed the response.

        # Must return an iterable of Request, or item objects.
        for i in result:
            yield i

    def process_spider_exception(self, response, exception, spider):
        # Called when a spider or process_spider_input() method
        # (from other spider middleware) raises an exception.

        # Should return either None or an iterable of Request or item objects.
        pass

    async def process_start(self, start):
        # Called with an async iterator over the spider start() method or the
        # maching method of an earlier spider middleware.
        async for item_or_request in start:
            yield item_or_request

    def spider_opened(self, spider):
        spider.logger.info("Spider opened: %s" % spider.name)


class WillhabenDownloaderMiddleware:
    # Not all methods need to be defined. If a method is not defined,
    # scrapy acts as if the downloader middleware does not modify the
    # passed objects.

    @classmethod
    def from_crawler(cls, crawler):
        # This method is used by Scrapy to create your spiders.
        s = cls()
        crawler.signals.connect(s.spider_opened, signal=signals.spider_opened)
        return s

    def process_request(self, request, spider):
        # Called for each request that goes through the downloader
        # middleware.

        # Must either:
        # - return None: continue processing this request
        # - or return a Response object
        # - or return a Request object
        # - or raise IgnoreRequest: process_exception() methods of
        #   installed downloader middleware will be called
        return None

    def process_response(self, request, response, spider):
        # Called with the response returned from the downloader.

        # Must either;
        # - return a Response object
        # - return a Request object
        # - or raise IgnoreRequest
        return response

    def process_exception(self, request, exception, spider):
        # Called when a download handler or a process_request()
        # (from other downloader middleware) raises an exception.

        # Must either:
        # - return None: continue processing this exception
        # - return a Response object: stops process_exception() chain
        # - return a Request object: stops process_exception() chain
        pass

    def spider_opened(self, spider):
        spider.logger.info("Spider opened: %s" % spider.name)


class ThrottlingDownloaderMiddleware:
    """Downloader middleware that adds a delay before each request.

    - Uses `SCRAPE_BASE_DELAY` (seconds) as the default pause between requests.
    - Every 10th request will wait `SCRAPE_LONG_DELAY` seconds instead.

    Implemented using Twisted's reactor so it doesn't block the event loop.
    """

    def __init__(self, crawler):
        self.crawler = crawler
        settings = crawler.settings
        self.base_delay = float(settings.getfloat("SCRAPE_BASE_DELAY", 1.0))
        self.long_delay = float(settings.getfloat("SCRAPE_LONG_DELAY", 10.0))
        self._counter = 0
        # maximum number of requests to allow (0 = no limit)
        self.max_scrapes = int(settings.getint("SCRAPE_MAX", 0))

    @classmethod
    def from_crawler(cls, crawler):
        mw = cls(crawler)
        crawler.signals.connect(mw.spider_opened, signal=signals.spider_opened)
        return mw

    def spider_opened(self, spider):
        spider.logger.info("ThrottlingDownloaderMiddleware enabled: base=%s long=%s", self.base_delay, self.long_delay)

    def process_request(self, request, spider):
        # increment counter for every outgoing request
        r  = random.randrange(1, 5)
        self._counter += 1
        # If max_scrapes is set and we've exceeded it, stop the spider.
        # Allow up to `max_scrapes` requests; close on the one after.
        if self.max_scrapes and self._counter > self.max_scrapes:
            raise CloseSpider(f"Reached configured max scrapes: {self.max_scrapes}")
        # choose delay
        if self._counter % 10 == 0:
            delay = self.long_delay
        else:
            delay = self.base_delay

        # Delay using Twisted reactor Deferred so we don't block
        from twisted.internet import reactor
        from twisted.internet.defer import Deferred

        d = Deferred()
        reactor.callLater(delay + r, d.callback, None)
        return d
