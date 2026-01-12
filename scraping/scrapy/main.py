import argparse
import os

from scrapy.crawler import CrawlerProcess
from scrapy.utils.log import configure_logging
from scrapy.utils.project import get_project_settings

from willhaben.spiders.immoscout import ImmoscoutSpider
from willhaben.spiders.willhaben import WillhabenSpider


def run_spiders(spiders):
	"""Run the given spider classes concurrently in one Scrapy process."""

	os.environ.setdefault("SCRAPY_SETTINGS_MODULE", "willhaben.settings")
	settings = get_project_settings()
	configure_logging({'LOG_FORMAT': '%(levelname)s: %(message)s'})

	process = CrawlerProcess(settings)
	for spider_cls in spiders:
		process.crawl(spider_cls)

	process.start()  # Blocks until all crawls finish


def main(argv=None):
	parser = argparse.ArgumentParser(description="Run willhaben and immoscout spiders")
	parser.add_argument(
		"--spiders",
		nargs="+",
		choices=["willhaben", "immoscout", "all"],
		default=["all"],
		help="Which spiders to run. Default runs both.",
	)

	args = parser.parse_args(argv)

	selected = set(args.spiders)
	if "all" in selected:
		spiders = [WillhabenSpider, ImmoscoutSpider]
	else:
		spiders = []
		if "willhaben" in selected:
			spiders.append(WillhabenSpider)
		if "immoscout" in selected:
			spiders.append(ImmoscoutSpider)

	if not spiders:
		parser.error("No spiders selected")

	run_spiders(spiders)


if __name__ == "__main__":
	main()
