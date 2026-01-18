# Scrapy Project Development Guide

This guide covers development, extending, and troubleshooting the real estate scraper.

## Table of Contents

- [Project Architecture](#project-architecture)
- [Development Setup](#development-setup)
- [Creating New Spiders](#creating-new-spiders)
- [Adding Pipelines](#adding-pipelines)
- [Data Flow](#data-flow)
- [Common Tasks](#common-tasks)
- [Debugging](#debugging)
- [Performance Optimization](#performance-optimization)

## Project Architecture

### Component Overview

```
                         ┌──────────────┐
                         │   Spiders    │
                         │  (willhaben, │
                         │  immoscout)  │
                         └──────┬───────┘
                                │
                                ▼
                    ┌────────────────────────┐
                    │  Item (BaseListingItem)│
                    │  - Raw data            │
                    │  - URLs, titles, etc.  │
                    └────────────┬───────────┘
                                 │
                ┌────────────────┼────────────────┐
                │                │                │
                ▼                ▼                ▼
          ┌──────────┐    ┌──────────┐    ┌──────────┐
          │ Pipeline │    │ Pipeline │    │ Pipeline │
          │ 300      │    │ 350      │    │ 375      │
          │Cleaning  │───▶│Validation│───▶│Dedupe    │
          └──────────┘    └──────────┘    └──────────┘
                                             │
                                             ▼
                                   ┌──────────────────┐
                                   │  CSV/Database    │
                                   │  Export          │
                                   └──────────────────┘
```

### Key Classes

- **BaseListingItem**: Core data model inheriting from dataclass
- **WillhabenItem**: Willhaben-specific listing
- **ImmoscoutItem**: Immoscout-specific listing
- **WillhabenSpider**: Main Willhaben crawler (with browser automation)
- **ImmoscoutSpider**: Immoscout crawler
- **Pipelines**: Data processing and validation stages

## Development Setup

## Creating New Spiders

### Basic Spider Template

## Data Flow

### Item Lifecycle

1. **Extraction** (Spider)
   ```
   Raw HTML → parse() → BaseListingItem(raw_size="100 m²", ...)
   ```

2. **Normalization** (Pipeline 300)
   ```
   raw_size="100 m²" → size_m2=100.0
   ```

3. **Validation** (Pipeline 350)
   ```
   Check size_m2 > 0, rooms >= 0, price >= 0
   ```

4. **Deduplication** (Pipeline 375)
   ```
   Compare location + buckets → drop if duplicate
   ```

5. **Export** (CSV/Database Pipeline 400)
   ```
   Final item → willhaben_output.csv and merge with neo4j
   ```

### Add Custom Field Processing

```python
class AreaNormalizationPipeline:
    """Convert different area formats to m²."""

    def process_item(self, item, spider):
        raw_size = item.get('raw_size', '')
        
        # Handle different formats
        if 'ha' in raw_size.lower():  # hectares
            value = float(raw_size.split()[0]) * 10000
        elif 'm²' in raw_size or 'm2' in raw_size.lower():
            value = float(re.search(r'\d+', raw_size).group())
        else:
            value = None
        
        item['size_m2'] = value
        return item
```

## Performance Optimization

### Concurrent Requests

Adjust in `willhaben/settings.py`:

```python
# Default: 16, adjust based on target site politeness
CONCURRENT_REQUESTS = 8

# Per-domain limit (recommended)
CONCURRENT_REQUESTS_PER_DOMAIN = 4
```

### Download Delay

```python
# Delay between requests (in seconds)
DOWNLOAD_DELAY = 2

# Also notice BaseDelay from Reactor further enhancing anti bot detection with custom middleware
SCRAPE_BASE_DELAY = 5
# Longer delay applied to every 10th request
SCRAPE_LONG_DELAY = 300
```
## Further Resources
- [Scrapy Documentation](https://docs.scrapy.org/)
- [Playwright Documentation](https://playwright.dev/python/)
- [Neo4j Python Driver](https://neo4j.com/docs/api/python/current/)
