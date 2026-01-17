"""
Willhaben Scrapy Project

A Scrapy-based web scraper for collecting real estate listings from Austrian
property platforms (Willhaben, Immoscout).

Key Components:
    - Spiders: Extract listings from target websites
    - Items: Standardized data models (BaseListingItem)
    - Pipelines: Data cleaning, validation, deduplication
    - Settings: Configuration for scrapy behavior

Entry Point:
    python main.py [--spiders {willhaben,immoscout,all}]
"""

__version__ = "1.0.0"
