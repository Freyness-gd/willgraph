"""
Data models for scraped real estate listings.

This module defines standardized item structures for property listings from
multiple sources (Willhaben, Immoscout). A unified schema (`BaseListingItem`)
allows heterogeneous data integration while supporting source-specific fields.

The items follow a lifecycle:
1. Raw scraped values captured as strings during spider execution
2. Pipelines normalize values (parse numbers, validate ranges)
3. Enrichment pipelines add geocoding and deduplication

See documentation: https://docs.scrapy.org/en/latest/topics/items.html
"""

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
    source: str = field(init=False, default="willhaben")
    """
	Property listing from Willhaben platform.

	Inherits unified schema from BaseListingItem with Willhaben-specific
	source identifier. Additional Willhaben-specific fields can be added here.

	Attributes:
	    source (str): Always "willhaben" for listings from this platform.
	"""


@dataclass
class ImmoscoutItem(BaseListingItem):
    mega_listing: Optional[bool] = None  # flag for multi-unit listings
    source: str = field(init=False, default="immoscout")
    """
	Property listing from Immoscout platform.

	Inherits unified schema from BaseListingItem with Immoscout-specific
	source identifier. Includes platform-specific fields like mega_listing
	for multi-unit properties.

	Attributes:
	    mega_listing (bool, optional): Flag indicating multi-unit/mega listing.
	    source (str): Always "immoscout" for listings from this platform.
	"""

