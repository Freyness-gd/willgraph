# Define here the models for your scraped items
#
# See documentation in:
# https://docs.scrapy.org/en/latest/topics/items.html

from dataclasses import dataclass, field, asdict
from datetime import datetime
from typing import Optional

import scrapy


@dataclass
class WillhabenItem:
    """Dataclass representing a willhaben real estate item."""
    url: str
    title: str
    location: str

    # raw scraped strings
    raw_size: Optional[str] = None  # e.g. "32 m²"
    rooms: Optional[int] = None  # e.g. "1"
    price_raw: Optional[str] = None  # e.g. "€ 1.250"

    # cleaned values (filled by pipeline)
    size_m2: Optional[float] = None
    price_eur: Optional[float] = None

    scraped_at: Optional[datetime] = None

    osm_id: Optional[str] = None
    lat: Optional[str] = None
    lon: Optional[str] = None

    # probably useless
    def to_scrapy_item(self) -> scrapy.Item:
        """Convert the dataclass to a plain `scrapy.Item` (dict-like).

        This helper is convenient for passing the item into existing
        Scrapy pipelines that expect `scrapy.Item` or mapping objects.
        """
        data = asdict(self)
        si = scrapy.Item()
        for k, v in data.items():
            si.setdefault(k, v)
        return si



@dataclass
class ImmoscoutItem:
    """Dataclass representing a immoscout real estate item."""
    url: str
    title: str
    location: str

    raw_ul: Optional[bool] = None

    # raw scraped strings
    raw_size: Optional[str] = None  # e.g. "32 m²"
    raw_rooms: Optional[float] = None  # e.g. "1"
    price_raw: Optional[str] = None  # e.g. "€ 1.250"

    # cleaned values (filled by pipeline)
    size_m2: Optional[float] = None
    price_eur: Optional[float] = None
    rooms: Optional[int] = None
    scraped_at: Optional[datetime] = None

    osm_id: Optional[str] = None
    lat: Optional[str] = None
    lon: Optional[str] = None

