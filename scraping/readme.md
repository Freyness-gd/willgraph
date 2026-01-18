# Scraping Module

This module contains web scrapers for real estate data collection from Austrian property platforms and public transportation data processing.

## Directory Structure

```
scraping/
├── scrapy/              # Scrapy-based web scraper for real estate listings
├── transport/           # GTFS transport data processor
├── Dockerfile          # Container setup for scraping service
├── requirements.txt    # Python dependencies
└── readme.md          # This file
```

## Scrapy Project

### Overview

A multi-spider Scrapy project that scrapes real estate listings from:
- **Willhaben** (willhaben.at) - Austrian real estate portal
- **Immoscout** - Property search platform

The scrapers collect property listings with details like location, size, rooms, price, and geocoded coordinates.

### Features

- **Multi-spider crawling**: Run willhaben, immoscout, or both simultaneously
- **Browser automation**: Uses Playwright for JavaScript-heavy sites
- **Data enrichment**: 
  - Reverse geocoding via Nominatim to validate coordinates
  - Deduplication to prevent duplicate entries
  - Data validation and normalization
- **CSV export**: Outputs standardized listings to CSV files
- **Neo4j integration**: Ready for graph database storage (pipelines configured)

### Project Structure

```
scrapy/
├── main.py                          # Entry point and CLI argument handling
├── scrapy.cfg                       # Scrapy project configuration
├── willhaben/
│   ├── items.py                    # Data models (BaseListingItem)
│   ├── settings.py                 # Spider settings and middleware config
│   ├── pipelines.py                # Data processing pipelines
│   ├── middlewares.py              # Custom middleware
│   ├── scriptToParseOsmId.py       # OpenStreetMap ID parsing utility
│   ├── remove_url.py               # URL processing utility
│   └── spiders/
│       ├── willhaben.py            # Willhaben spider with browser automation
│       ├── willhaben_details.py    # Detailed listing scraper
│       └── immoscout.py            # Immoscout spider
├── immoscout_output.csv            # Latest Immoscout scrape output
└── willhaben_output.csv            # Latest Willhaben scrape output
```

### Data Model

Listings are standardized as `BaseListingItem` with:

**Required fields:**
- `url`: Property listing URL
- `title`: Property title/name
- `location`: Address or location string

**Raw fields** (as scraped):
- `raw_size`: Size string (e.g., "100 m²")
- `raw_rooms`: Room count string (e.g., "3")
- `price_raw`: Price string

**Normalized fields** (processed by pipelines):
- `size_m2`: Size in square meters (float)
- `rooms`: Number of rooms (float)
- `price_eur`: Price in EUR (float)

**Enrichment fields**:
- `osm_id`: OpenStreetMap identifier
- `lat`, `lon`: Geocoded coordinates

**Metadata**:
- `scraped_at`: Timestamp
- `source`: Data source identifier


**Key settings** in `willhaben/settings.py`:
- `ROBOTSTXT_OBEY`: Does not use robots.txt
- `CONCURRENT_REQUESTS`: Parallel request limit
- `DOWNLOAD_DELAY`: Delay between requests (politeness and anti bot detection)
- `USER_AGENT`: Identifies the scraper

### Pipelines

Data processing pipeline (in execution order):

1. **Willhaben/ImmoscoutPipeline** (i.e. BaseCleaningPipeline) (priority 300): Cleans and normalizes data
   - Parse numbers from strings
   - Extract location information
   - Handle missing values

2. **ValidationPipeline** (priority 350): Validates data quality
   - Checks required fields present
   - Validates data types and ranges
   - Filters invalid entries

3. **DeduplicationPipeline** (priority 375): Removes duplicates
   - Compares listings by key fields
   - Prevents duplicate records

4. **Neo4jPipeline** (priority 400, commented out): Graph storage
   - Stores listings in Neo4j database
   - Uncomment to enable

#### Add a New Spider
1. Create a new spider class in `willhaben/spiders/` extending `scrapy.Spider`
2. Implement `start_requests()` or `start()` method
3. Implement `parse()` callback to extract data
4. Add spider import to `main.py`
5. Update CLI choices in `main.py`

#### Add Data Enrichment
Create a new pipeline in `willhaben/pipelines.py` with priority between 300-400:
```python
class MyPipeline:
    def process_item(self, item, spider):
        # Process item
        return item
```

Register in spider's `custom_settings`:
```python
"willhaben.pipelines.MyPipeline": 360,  # Between 350 and 375
```
### VNC Access

To solve the captchas in our docker environment visit following page after docker compose up:
http://localhost:6080/vnc.html