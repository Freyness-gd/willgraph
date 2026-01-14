import csv
import os
from neo4j import GraphDatabase

BASE_DIR = './gtfs_data'
OUTPUT_NODES = 'transport_nodes.csv'
OUTPUT_EDGES = 'transport_edges.csv'
URI = os.getenv("NEO4J_URI", "bolt://localhost:7687")
NEO4J_USER = os.getenv("NEO4J_USER", "neo4j")
NEO4J_PASSWORD = os.getenv("NEO4J_PASSWORD", "testpassword")
AUTH = (NEO4J_USER, NEO4J_PASSWORD)

BATCH_SIZE = 1000

def import_data():
    driver = GraphDatabase.driver(URI, auth=AUTH)
    with driver.session() as session:
        print("Creating Indexes...")
        create_constraints(session)

        print("Importing Nodes...")
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
        print("Importing Edges...")
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
        driver.close()
        print("Done.")
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
            r.frequency = row.freq
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
        t.version = 0
    """
    session.run(query, batch=batch)

if __name__ == "__main__":
    import_data()