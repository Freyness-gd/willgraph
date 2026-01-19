# WillGraph

A graph-based real estate search platform for Austria that combines property listings with public transportation and points of interest (POI) data. Built with Neo4j graph database, Spring Boot backend, and Quasar/Vue.js frontend.

## Features

- 🏠 **Real Estate Listings** - Scraped from Austrian property platforms (Willhaben, Immoscout)
- 🚇 **Public Transport Integration** - GTFS data from Austrian transport networks
- 📍 **POI Search** - Find nearby amenities (shops, schools, healthcare, etc.)
- 🗺️ **Interactive Map** - Leaflet-based visualization with heatmaps
- 🔍 **Smart Search** - Filter properties by price, size, transport accessibility, and nearby amenities

## Architecture

| Service | Description | Port |
|---------|-------------|------|
| **neo4j** | Graph database for storing and querying data | 7474 (UI), 7687 (Bolt) |
| **data-importer** | Imports GTFS transport data into Neo4j | - |
| **scraping** | Web scraper for real estate listings | 6080 (noVNC) |
| **backend** | Spring Boot REST API | 8080 |
| **webapp** | Quasar/Vue.js frontend | 80 |

## Prerequisites

- [Docker](https://docs.docker.com/get-docker/) and [Docker Compose](https://docs.docker.com/compose/install/)
- At least 4GB of available RAM for Neo4j

## Quick Start

### 1. Clone the repository

```bash
git clone https://github.com/Freyness-gd/willgraph.git
cd willgraph
```

### 2. Configure environment (optional)

Create or edit the transport data environment file:

```bash
cp scraping/transport/.env.example scraping/transport/.env
```

### 3. Start all services

```bash
docker compose up -d
```

This will:
1. Start **Neo4j** database and wait until healthy
2. Run **data-importer** to load GTFS transport data into Neo4j
3. Start **scraping** service for real estate data collection
4. Launch **backend** Spring Boot API (waits for Neo4j and data-importer)
5. Start **webapp** frontend (waits for backend to be healthy)

### 4. Access the application

Once all containers are running, open your browser:

| Service | URL |
|---------|-----|
| **Frontend** | [http://localhost](http://localhost) |
| **Backend API** | [http://localhost:8080/api](http://localhost:8080/api) |
| **Neo4j Browser** | [http://localhost:7474](http://localhost:7474) |
| **Scraping VNC** | [http://localhost:6080/vnc.html](http://localhost:6080/vnc.html) |

> **Note:** The frontend may take a few minutes to become available while the backend initializes and imports data.

### 5. Check container status

```bash
docker compose ps
```

### 6. View logs

```bash
# All services
docker compose logs -f

# Specific service
docker compose logs -f webapp
docker compose logs -f backend
```

### 7. Stop all services

```bash
docker compose down
```

To also remove the Neo4j data volume:

```bash
docker compose down -v
```

## Development

### Running services individually

```bash
# Start only the database
docker compose up -d neo4j

# Start backend in dev mode (requires local Java 25+)
cd backend && ./gradlew bootRun

# Start frontend in dev mode (requires Node.js 20+)
cd webapp && yarn install && yarn dev
```

### Neo4j Credentials

- **Username:** `neo4j`
- **Password:** `testpassword`

## Project Structure

```
willgraph/
├── backend/          # Spring Boot REST API
├── scraping/         # Python scrapers and data importers
│   ├── scrapy/       # Willhaben/Immoscout web scraper
│   └── transport/    # GTFS data processor
├── webapp/           # Quasar/Vue.js frontend
├── compose.yaml      # Docker Compose configuration
└── README.md
```

## License

See [LICENSE](LICENSE) for details.

