import csv
import os
import logging
import hashlib
import json
import sys
from neo4j import GraphDatabase

logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s [%(levelname)s] %(message)s',
    handlers=[logging.StreamHandler(sys.stdout)]
)
logger = logging.getLogger(__name__)

BASE_DIR = './gtfs_data'
OUTPUT_NODES = os.path.join(BASE_DIR, 'transport_nodes.csv')
OUTPUT_EDGES = os.path.join(BASE_DIR, 'transport_edges.csv')
STATE_FILE = os.path.join(BASE_DIR, 'import_state.json')

URI = os.getenv("NEO4J_URI", "bolt://localhost:7687")
NEO4J_USER = os.getenv("NEO4J_USER", "neo4j")
NEO4J_PASSWORD = os.getenv("NEO4J_PASSWORD", "testpassword")
AUTH = (NEO4J_USER, NEO4J_PASSWORD)

BATCH_SIZE = 1000

def calculate_file_hash(filepaths):
    """Generates an MD5 hash for a list of files to detect changes."""
    hasher = hashlib.md5()
    for filepath in filepaths:
        if not os.path.exists(filepath):
            continue
        with open(filepath, 'rb') as f:
            while chunk := f.read(8192):
                hasher.update(chunk)
    return hasher.hexdigest()

def is_local_data_changed(current_hash):
    """Checks if the data has changed since the last run."""
    if not os.path.exists(STATE_FILE):
        return True

    try:
        with open(STATE_FILE, 'r') as f:
            state = json.load(f)
            last_hash = state.get('data_hash')
            return last_hash != current_hash
    except Exception as e:
        logger.warning(f"Could not read state file, forcing refresh: {e}")
        return True

    return True
def is_db_populated(driver):
    """Checks if the Neo4j database actually has data."""
    try:
        with driver.session() as session:
            result = session.run("MATCH (t:Transport) RETURN count(t) AS count")
            record = result.single()
            return record["count"] > 0
    except Exception as e:
        logger.warning(f"Could not check DB state (assuming empty): {e}")
        return False

def update_state(current_hash):
    """Updates the state file after a successful import."""
    with open(STATE_FILE, 'w') as f:
        json.dump({'data_hash': current_hash}, f)
    logger.info("State saved.")

def import_data():
    if not os.path.exists(OUTPUT_NODES) or not os.path.exists(OUTPUT_EDGES):
        logger.error("CSV files not found. Did gtfs_processor.py run?")
        sys.exit(1)

    driver = GraphDatabase.driver(URI, auth=AUTH)
    try:
        driver.verify_connectivity()
        current_hash = calculate_file_hash([OUTPUT_NODES, OUTPUT_EDGES])
        files_changed = is_local_data_changed(current_hash)
        db_has_data = is_db_populated(driver)
        if db_has_data and not files_changed:
            logger.info("Database is populated and files haven't changed. Skipping import.")
            sys.exit(0)
        if not db_has_data:
            logger.info("  Database appears empty. Forcing import...")
        elif files_changed:
            logger.info("  New CSV data detected. Starting update...")

        with driver.session() as session:
            logger.info("Creating Indexes...")
            create_constraints(session)

            logger.info("Importing Nodes...")
            with open(OUTPUT_NODES, 'r', encoding="utf-8") as f:
                reader = csv.DictReader(f)
                batch = []
                for row in reader:
                    row['lat'] = float(row['stop_lat'])
                    row['lon'] = float(row['stop_lon'])
                    batch.append(row)

                    if len(batch) >= BATCH_SIZE:
                        insert_nodes(session, batch)
                        batch = []
                if batch: insert_nodes(session, batch)

            logger.info("Importing Edges...")
            with open(OUTPUT_EDGES, 'r', encoding="utf-8") as f:
                reader = csv.DictReader(f)
                batch = []
                for row in reader:
                    row['weight'] = float(row['weight_minutes'])
                    row['freq'] = int(float(row['frequency']))
                    batch.append(row)

                    if len(batch) >= BATCH_SIZE:
                        insert_edges(session, batch)
                        batch = []
                if batch: insert_edges(session, batch)
        update_state(current_hash)
        logger.info("Done.")
    except Exception as e:
        logger.error(f"Import failed: {e}")
        sys.exit(1)
    finally:
        driver.close()
def create_constraints(session):
    session.run("CREATE CONSTRAINT transport_id_unique IF NOT EXISTS FOR (t:Transport) REQUIRE t.id IS UNIQUE")
    session.run("CREATE INDEX transport_location_index IF NOT EXISTS FOR (t:Transport) ON (t.location)")

def insert_edges(session, batch):
    query = """
        UNWIND $batch AS row
        MATCH (source:Transport {id: row.source_id})
        MATCH (target:Transport {id: row.target_id})
        MERGE (source)-[r:CONNECTED_TO]->(target)
        SET r.travelTimeInMinutes = row.weight,
            r.frequency = row.freq,
            r.last_updated = datetime()
        """
    session.run(query, batch=batch)

def insert_nodes(session, batch):
    query = """
    UNWIND $batch AS row
    MERGE (t:Transport {id: row.stop_id}) 
    ON CREATE SET 
        t.name = row.stop_name,
        t.location = point({latitude: row.lat, longitude: row.lon}),
        t.type = 'Station',
        t.version = 1,
        t.created_at = datetime()
        
    ON MATCH SET 
        t.name = row.stop_name,
        t.location = point({latitude: row.lat, longitude: row.lon}),
        t.version = coalesce(t.version, 0) + 1,
        t.updated_at = datetime()
    """
    session.run(query, batch=batch)

if __name__ == "__main__":
    import_data()