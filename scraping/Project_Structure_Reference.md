# Project Structure Guide

Complete map of the scrapy project directory with descriptions of each file and folder.

## Directory Tree

```
scraping/
├── scrapy/                          # Main Scrapy project package
│   ├── __init__.py                 # Package initialization
│   ├── main.py                     # CLI entry point
│   ├── scrapy.cfg                  # Scrapy project settings
│   ├── willhaben/                  # Spider and pipeline module
│   │   ├── __init__.py
│   │   ├── items.py                # Data models
│   │   ├── settings.py             # Scrapy configuration
│   │   ├── pipelines.py            # Data processing pipelines
│   │   ├── middlewares.py          # Custom middleware
│   │   ├── scriptToParseOsmId.py   # OpenStreetMap utilities
│   │   ├── remove_url.py           # URL processing
│   │   ├── spiders/
│   │   │   ├── __init__.py
│   │   │   ├── willhaben.py        # Willhaben spider
│   │   │   ├── willhaben_details.py # Details scraper
│   │   │   └── immoscout.py        # Immoscout spider
│   │   └── __pycache__/
│   ├── willhaben_output.csv        # Willhaben scrape results
│   └── immoscout_output.csv        # Immoscout scrape results
├── transport/                       # GTFS transport data processor (separate)
├── Dockerfile                       # Docker configuration
├── requirements.txt                # Python dependencies
├── readme.md                        # Main documentation
├── QUICKSTART.md                    # 5-minute setup guide
├── DEVELOPMENT.md                   # Development guide
├── API_REFERENCE.md                 # API documentation
└── PROJECT_STRUCTURE.md             # This file
```

## File Descriptions

### Core Project Files

### Data Models

#### `willhaben/items.py`
**Purpose**: Define item data structures  
**Size**: ~110 lines  
**Classes**:
- `BaseListingItem`: Base dataclass with common fields
  - Required: url, title, location
  - Raw fields: raw_size, raw_rooms, price_raw
  - Normalized: size_m2, rooms, price_eur
  - Enrichment: osm_id, lat, lon, scraped_at, source
- `WillhabenItem`: Willhaben-specific subclass
- `ImmoscoutItem`: Immoscout-specific subclass with mega_listing field

**Key Methods**:
- `as_record()`: Export to dict
- `to_scrapy_item()`: Convert to Scrapy Item

### Configuration

#### `willhaben/settings.py`
**Purpose**: Scrapy framework configuration  
**Size**: ~50-100 lines (varies)  
**Key Settings**:
- `CONCURRENT_REQUESTS`: Parallel request limit
- `DOWNLOAD_DELAY`: Politeness delay
- `USER_AGENT`: Bot identifier
- `ROBOTSTXT_OBEY`: Respect robots.txt
- `ITEM_PIPELINES`: Processing pipeline order
- `FEEDS`: Output format configuration

**Typical Content**:
```python
BOT_NAME = 'willhaben'
SPIDER_MODULES = ['willhaben.spiders']
NEWSPIDER_MODULE = 'willhaben.spiders'
CONCURRENT_REQUESTS = 16
DOWNLOAD_DELAY = 1
```

### Data Processing

#### `willhaben/pipelines.py`
**Purpose**: Data processing pipeline implementations  
**Classes** (typical):
- `WillhabenPipeline` (priority 300): Normalize raw values
  - Parse numbers from strings
  - Handle missing data
  - Basic validation
- `ValidationPipeline` (priority 350): Validate data quality
  - Check required fields
  - Validate ranges (size > 0, price >= 0)
  - Filter invalid entries
- `DeduplicationPipeline` (priority 375): Remove duplicates
  - Track seen URLs
  - Compare key fields
- `Neo4jPipeline` (priority 400, optional): Store in graph database

**Execution Order**:
```
Item → Pipeline300 → Pipeline350 → Pipeline375 → Pipeline400 → CSV/Output
       (clean)     (validate)    (dedupe)     (store)
```

#### `willhaben/middlewares.py`
**Purpose**: Custom request/response middleware  
**Typical Use**:
- Custom headers
- Request throttling
- Response handling
- Error recovery

### Spiders

#### `willhaben/spiders/willhaben.py`
**Purpose**: Scrape Willhaben.at real estate listings  
**Key Methods**:
- `start_requests()`: Generate initial requests
- `parse(response)`: Extract listing links
- `parse_listing(response)`: Extract listing details
- Helper methods for location/geocoding

**Features**:
- Browser automation with Playwright
- Slow scrolling for dynamic content
- Nominatim reverse geocoding integration
- Location validation and caching

#### `willhaben/spiders/immoscout.py`
**Purpose**: Scrape Immoscout property listings  
**Key Methods**:
- Similar structure to willhaben spider
- Platform-specific field extraction

#### `willhaben/spiders/willhaben_details.py`
**Purpose**: Detailed listing information scraper  
**Use**: Extract exact location from leaflet

### Utilities

#### `willhaben/scriptToParseOsmId.py`
**Purpose**: Parse and work with OpenStreetMap IDs  
**Typical Usage**:
- Extract OSM feature IDs from geocoding results
- Validate OSM data


### Output

#### `scrapy/willhaben_output.csv`
**Purpose**: Most recent Willhaben scraped listings
**Format**: CSV with fields:
```csv
url,title,location,raw_size,raw_rooms,price_raw,size_m2,rooms,price_eur,osm_id,lat,lon,scraped_at,source
https://willhaben.at/...,Apartment,Wien,100 m²,3,450.000 €,100.0,3.0,450000.0,...,48.2,16.3,2025-01-17T...,willhaben
```

#### `scrapy/immoscout_output.csv`
**Purpose**: Most recent Immoscout scraped listings  
**Format**: Same as willhaben_output.csv with source="immoscout"

### Documentation

#### `readme.md`
**Purpose**: Main project documentation  
**Sections**:
- Project overview
- Features and architecture


## Data Flow Visualization

```
┌─────────────────────┐
│ start_requests()    │  main.py triggers spider
└──────────┬──────────┘
           │
           ▼
┌──────────────────────┐
│ Fetch HTML pages     │  HTTP requests to target sites
└──────────┬───────────┘
           │
           ▼
┌──────────────────────┐
│ parse() method       │  Extract data from HTML
└──────────┬───────────┘
           │ yields
           ▼
┌──────────────────────┐
│ BaseListingItem      │  Raw item with string values
└──────────┬───────────┘
           │
           ▼
┌──────────────────────────────────────────────────────┐
│              PIPELINES (priority order)              │
├──────────────────────────────────────────────────────┤
│ 300: WillhabenPipeline         → Normalize values    │
├──────────────────────────────────────────────────────┤
│ 350: ValidationPipeline        → Validate data       │
├──────────────────────────────────────────────────────┤
│ 375: DeduplicationPipeline     → Remove duplicates   │
├──────────────────────────────────────────────────────┤
│ 400: Neo4jPipeline (optional)  → Store in database   │
└──────────┬───────────────────────────────────────────┘
           │
           ▼
┌──────────────────────┐
│ CSV Export           │  willhaben_output.csv
└──────────────────────┘
```