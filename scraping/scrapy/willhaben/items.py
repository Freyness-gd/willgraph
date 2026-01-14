# Define here the models for your scraped items
#
# See documentation in:
# https://docs.scrapy.org/en/latest/topics/items.html

from dataclasses import dataclass, field, asdict
from datetime import datetime
from typing import Optional

import scrapy


@dataclass
class BaseListingItem:
    """Shared, source-agnostic listing shape for heterogeneous integration."""

    # Required identifying fields
    url: str
    title: str
    location: str

    # Raw scraped values (parsed/cleaned in pipelines)
    raw_size: Optional[str] = None
    raw_rooms: Optional[str] = None
    price_raw: Optional[str] = None

    # Normalized values (produced by pipelines)
    size_m2: Optional[float] = None
    rooms: Optional[float] = None
    price_eur: Optional[float] = None

    # Enrichment / geocoding
    osm_id: Optional[str] = None
    lat: Optional[str] = None
    lon: Optional[str] = None

    # Metadata
    scraped_at: datetime = field(default_factory=datetime.utcnow)
    source: str = field(init=False, default="listing")

    def to_scrapy_item(self) -> scrapy.Item:
        """Convert the dataclass to a plain `scrapy.Item` for pipeline reuse."""
        item = scrapy.Item()
        for key, value in asdict(self).items():
            item.setdefault(key, value)
        return item

    def as_record(self) -> dict:
        """Return a plain dict for storage or export layers."""
        return asdict(self)


@dataclass
class WillhabenItem(BaseListingItem):
    """Willhaben listing with the unified schema used across sources."""

    source: str = field(init=False, default="willhaben")


@dataclass
class ImmoscoutItem(BaseListingItem):
    """Immoscout listing with the unified schema used across sources."""

    mega_listing: Optional[bool] = None  # flag for multi-unit listings
    source: str = field(init=False, default="immoscout")

