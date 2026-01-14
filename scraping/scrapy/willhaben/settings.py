# Scrapy settings for willhaben project
#
# For simplicity, this file contains only settings considered important or
# commonly used. You can find more settings consulting the documentation:
#
#     https://docs.scrapy.org/en/latest/topics/settings.html
#     https://docs.scrapy.org/en/latest/topics/downloader-middleware.html
#     https://docs.scrapy.org/en/latest/topics/spider-middleware.html

from shutil import which


BOT_NAME = "willhaben"

SPIDER_MODULES = ["willhaben.spiders"]
NEWSPIDER_MODULE = "willhaben.spiders"

ADDONS = {}

LOG_LEVEL = "INFO"

NEO4J_URI = "neo4j://localhost:7687"
NEO4J_USER = "neo4j"
NEO4J_PASSWORD = "testpassword"
NEO4J_DATABASE = "neo4j"

# Crawl responsibly by identifying yourself (and your website) on the user-agent
#USER_AGENT = "willhaben (+http://www.yourdomain.com)"

# Obey robots.txt rules
ROBOTSTXT_OBEY = False

# Concurrency and throttling settings
#CONCURRENT_REQUESTS = 16
CONCURRENT_REQUESTS_PER_DOMAIN = 1
# Base delay (seconds) between requests applied by the custom throttling
SCRAPE_BASE_DELAY = 5
# Longer delay applied to every 10th request
SCRAPE_LONG_DELAY = 300

# If you keep DOWNLOAD_DELAY, it will be combined with the middleware delay.
# Set DOWNLOAD_DELAY to 0 if you want only the middleware delays to apply.
DOWNLOAD_DELAY = 0

TWISTED_REACTOR = "twisted.internet.asyncioreactor.AsyncioSelectorReactor"




PLAYWRIGHT_LAUNCH_OPTIONS = {
    "headless": True,     # default for all spiders
}

PLAYWRIGHT_BROWSER_CONTEXTS = {
    "extra": {
        "user_agent": (
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
            "AppleWebKit/537.36 (KHTML, like Gecko) "
            "Chrome/120.0.0.0 Safari/537.36"
        ),
        "java_script_enabled": True,
    }
}

# Disable cookies (enabled by default)
#COOKIES_ENABLED = False

# Disable Telnet Console (enabled by default)
#TELNETCONSOLE_ENABLED = False

# Override the default request headers:
#DEFAULT_REQUEST_HEADERS = {
#    "Accept": "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8",
#    "Accept-Language": "en",
#}

# Enable or disable spider middlewares
# See https://docs.scrapy.org/en/latest/topics/spider-middleware.html
#SPIDER_MIDDLEWARES = {
#    "willhaben.middlewares.WillhabenSpiderMiddleware": 543,
#}

# Enable or disable downloader middlewares
# See https://docs.scrapy.org/en/latest/topics/downloader-middleware.html

# Enable or disable extensions
# See https://docs.scrapy.org/en/latest/topics/extensions.html
#EXTENSIONS = {
#    "scrapy.extensions.telnet.TelnetConsole": None,
#}

# Configure item pipelines
# See https://docs.scrapy.org/en/latest/topics/item-pipeline.html
ITEM_PIPELINES = {
    "willhaben.pipelines.WillhabenPipeline": 200,
    "willhaben.pipelines.ImmoscoutPipeline": 200,
    "willhaben.pipelines.ValidationPipeline": 250,
    "willhaben.pipelines.DeduplicationPipeline": 275,  # runs after validation, before Neo4j
    "willhaben.pipelines.Neo4jPipeline": 300,
}

DOWNLOAD_HANDLERS = {
    "http": "scrapy_playwright.handler.ScrapyPlaywrightDownloadHandler",
    "https": "scrapy_playwright.handler.ScrapyPlaywrightDownloadHandler",
}

# Enable our custom throttling middleware
DOWNLOADER_MIDDLEWARES = {
    # path: priority (lower value runs earlier)
    "willhaben.middlewares.ThrottlingDownloaderMiddleware": 543,
}

# Enable and configure the AutoThrottle extension (disabled by default)
# See https://docs.scrapy.org/en/latest/topics/autothrottle.html
#AUTOTHROTTLE_ENABLED = True
# The initial download delay
#AUTOTHROTTLE_START_DELAY = 5
# The maximum download delay to be set in case of high latencies
#AUTOTHROTTLE_MAX_DELAY = 60
# The average number of requests Scrapy should be sending in parallel to
# each remote server
#AUTOTHROTTLE_TARGET_CONCURRENCY = 1.0
# Enable showing throttling stats for every response received:
#AUTOTHROTTLE_DEBUG = False

# Enable and configure HTTP caching (disabled by default)
# See https://docs.scrapy.org/en/latest/topics/downloader-middleware.html#httpcache-middleware-settings
#HTTPCACHE_ENABLED = True
#HTTPCACHE_EXPIRATION_SECS = 0
#HTTPCACHE_DIR = "httpcache"
#HTTPCACHE_IGNORE_HTTP_CODES = []
#HTTPCACHE_STORAGE = "scrapy.extensions.httpcache.FilesystemCacheStorage"

# Set settings whose default value is deprecated to a future-proof value
FEED_EXPORT_ENCODING = "utf-8"

# Maximum number of requests (scrapes) to perform. 0 = unlimited
SCRAPE_MAX = 2
