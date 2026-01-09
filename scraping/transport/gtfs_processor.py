import csv

import pandas as pd
import numpy as np
import zipfile
import os
import glob
import io
import re
import shutil
import requests
from datetime import datetime
from dotenv import load_dotenv

load_dotenv()

BASE_DIR = './gtfs_data'
OUTPUT_NODES = 'transport_nodes.csv'
OUTPUT_EDGES = 'transport_edges.csv'
DBP_USERNAME = os.getenv("DBP_USERNAME", "")
DBP_PASSWORD = os.getenv("DBP_PASSWORD", "")

TOKEN_URL = "https://user.mobilitaetsverbuende.at/auth/realms/dbp-public/protocol/openid-connect/token"
API_BASE_URL = "https://data.mobilitaetsverbuende.at/api/public/v1/data-sets"
CLIENT_ID = "dbp-public-ui"

AUSTRIAN_STATES = [
  "Wien", "Niederösterreich", "Oberösterreich", "Salzburg",
  "Tirol", "Vorarlberg", "Kärnten", "Steiermark", "Burgenland",
  "Vienna", "Upper Austria", "Lower Austria", "Carinthia", "Styria", "Tyrol"
]
class DBPClient:
    def __init__(self, username, password):
        self.username = username
        self.password = password
        self.token = None

    def authenticate(self):
        print("Authenticating with DBP...")
        payload = {
            'client_id': CLIENT_ID,
            'username': self.username,
            'password': self.password,
            'grant_type': 'password',
            'scope': 'openid'
        }
        try:
            response = requests.post(TOKEN_URL, data=payload)
            response.raise_for_status()
            self.token = response.json().get('access_token')
            print("Authentication successful.")
        except Exception as e:
            print(f"Authentication failed, maybe .env is missing or not correct?: {e}")
            self.token = None

    def get_headers(self):
        return {
            'Authorization': f'Bearer {self.token}',
            'Accept': 'application/json'
        }

    def sync_regional_gtfs(self, output_base_dir):
        if not self.token: return

        url = f"{API_BASE_URL}?tagIds=20&tagFilterModeInclusive=true"
        print(f"Fetching dataset list from {url}...")
        try:
            response = requests.get(url, headers=self.get_headers(), verify=False)
            datasets = response.json()
        except Exception as e:
            print(f"Error listing datasets: {e}")
            return

        current_year = str(datetime.now().year)
        download_count = 0

        for ds in datasets:
            ds_name = ds.get('nameDe', '') or ds.get('name', '')
            ds_id = ds.get('id')

            #Exclusion properties
            if "flex" in ds_name.lower():
                print(f"Skipping (Excluded Name): {ds_name}")
                continue

            if "in sieben teilen" in ds_name.lower():
                print(f"Skipping (Excluded Split): {ds_name}")
                continue

            tags = ds.get('tags', [])
            tag_values = [t.get('valueDe', '') for t in tags] + [t.get('valueEn', '') for t in tags]
            is_regional = any(state.lower() in ds_name.lower() for state in AUSTRIAN_STATES) or \
                          any(state in tag_val for state in AUSTRIAN_STATES for tag_val in tag_values)
            if is_regional:
                print(f"Found Match: {ds_name} (ID: {ds_id})")
                self.download_and_extract(ds_id, ds_name, current_year, output_base_dir)
                download_count += 1
            else:
                pass

        print(f"Finished. Downloaded/Updated {download_count} datasets.")

    def download_and_extract(self, ds_id, dataset_name, current_year, output_base_dir):
        clean_name = re.sub(r'[^a-zA-Z0-9]', '_', dataset_name)
        target_dir = os.path.join(output_base_dir, f"{ds_id}_{clean_name}")
        if os.path.exists(target_dir):
            shutil.rmtree(target_dir)
        os.makedirs(target_dir, exist_ok=True)
        url = f"{API_BASE_URL}/{ds_id}/{current_year}/file"
        try:
            headers = self.get_headers()
            headers['Accept'] = 'application/zip'
            print(f"  Downloading ID {ds_id}...")
            response = requests.get(url, headers=headers, stream=True, verify=False)
            if response.status_code == 404:
                print(f"  Warning: No data found for year {current_year}. Trying previous year...")
                return
            response.raise_for_status()
            with zipfile.ZipFile(io.BytesIO(response.content)) as z:
                z.extractall(target_dir)
            print(f"  Extracted to: {target_dir}")
        except Exception as e:
            print(f"  Failed to download ID {ds_id}: {e}")

    def download_dataset(self, dataset_id, year, output_folder):
        """Downloads and extracts the dataset for a specific year."""
        if not self.token: return

        url = f"{API_BASE_URL}/{dataset_id}/{year}/file"
        print(f"Downloading dataset {dataset_id} for year {year}...")

        try:
            headers = self.get_headers()
            headers['Accept'] = 'application/zip'
            response = requests.get(url, headers=headers, stream=True, verify=False)
            response.raise_for_status()

            # Extract in memory
            with zipfile.ZipFile(io.BytesIO(response.content)) as z:
                z.extractall(output_folder)
            print(f"Extracted to {output_folder}")

        except Exception as e:
            print(f"Failed to download/extract: {e}")

def fetch_data_from_api():
    if not DBP_USERNAME or "EXAMPLE.COM" in DBP_USERNAME:
        print("Skipping API download (Credentials not set). Using local files.")
        return

    client = DBPClient(DBP_USERNAME, DBP_PASSWORD)
    client.authenticate()

    if not client.token:
        return

    client.sync_regional_gtfs(BASE_DIR)


def parse_gtfs_time(time_str):
    if pd.isna(time_str):
        return None
    try:
        h, m, s = map(int, time_str.split(':'))
        return h * 60 + m + s / 60.0  # Returns minutes from midnight
    except:
        return None

def process_gtfs():
    fetch_data_from_api()
    print("Loading GTFS data...")
    stop_files = glob.glob(os.path.join(BASE_DIR, '**', 'stops.txt'), recursive=True)
    time_files = glob.glob(os.path.join(BASE_DIR, '**', 'stop_times.txt'), recursive=True)

    if not stop_files:
        print(f"No stops.txt found in subdirectories of {BASE_DIR}")
        return

    print(f"Found {len(stop_files)} GTFS directories.")

    print("Loading stops from all sources...")
    stops_cols = ['stop_id', 'stop_name', 'stop_lat', 'stop_lon']

    stops_df_list = []
    for f in stop_files:
        print(f"  Reading {f}...")
        try:
            # Force stop_id to string to avoid mismatches
            df = pd.read_csv(f, usecols=lambda c: c in stops_cols, dtype={'stop_id': str})
            stops_df_list.append(df)
        except Exception as e:
            print(f"  Warning: Could not read {f}: {e}")

    if not stops_df_list:
        print("Error: No valid stops data found.")
        return

    all_stops = pd.concat(stops_df_list, ignore_index=True)
    all_stops.drop_duplicates(subset=['stop_id'], inplace=True)
    print(f"Total unique stops: {len(all_stops)}")

    print("Processing stop_times...")
    time_cols = ['trip_id', 'stop_id', 'arrival_time', 'departure_time', 'stop_sequence']

    all_edges = []

    for f in time_files:
        print(f"  Processing routes in {os.path.basename(os.path.dirname(f))}...")

        # Process file-by-file to keep RAM usage low
        try:
            # Load
            df = pd.read_csv(f, usecols=lambda c: c in time_cols,
                             dtype={'trip_id': str, 'stop_id': str, 'stop_sequence': 'int32'})

            # Sort sequence: Trip A -> Stop 1, Stop 2, Stop 3...
            df.sort_values(by=['trip_id', 'stop_sequence'], inplace=True)

            # Shift to get Target Node
            df['next_stop_id'] = df['stop_id'].shift(-1)
            df['next_trip_id'] = df['trip_id'].shift(-1)
            df['next_arrival_time'] = df['arrival_time'].shift(-1)

            # Filter valid connections (Same Trip Only)
            valid = df[df['trip_id'] == df['next_trip_id']].copy()
            del df  # Free RAM

            # Calculate Times
            valid['dep_min'] = valid['departure_time'].apply(parse_gtfs_time)
            valid['arr_min'] = valid['next_arrival_time'].apply(parse_gtfs_time)
            valid['duration'] = valid['arr_min'] - valid['dep_min']

            valid = valid[(valid['duration'] > 0) & (valid['duration'] < 300)]

            edges_agg = valid.groupby(['stop_id', 'next_stop_id']).agg(
                weight_minutes=('duration', 'mean'),
                frequency=('trip_id', 'count')
            ).reset_index()

            all_edges.append(edges_agg)

        except Exception as e:
            print(f"  Skipping {f} due to error: {e}")

    if not all_edges:
        print("Error: No edges created.")
        return

    print("Merging network...")
    total_edges = pd.concat(all_edges, ignore_index=True)

    final_edges = total_edges.groupby(['stop_id', 'next_stop_id']).agg(
        weight_minutes=('weight_minutes', 'mean'),
        frequency=('frequency', 'sum')
    ).reset_index()

    final_edges.columns = ['source_id', 'target_id', 'weight_minutes', 'frequency']

    #Only keep edges where both nodes are in our unique stops list
    valid_ids = set(all_stops['stop_id'])
    final_edges = final_edges[
        final_edges['source_id'].isin(valid_ids) &
        final_edges['target_id'].isin(valid_ids)
        ]

    print(f"Exporting {len(all_stops)} nodes and {len(final_edges)} edges...")
    all_stops.to_csv(OUTPUT_NODES, index=False)
    final_edges.to_csv(OUTPUT_EDGES, index=False)
    print("Done!")

if __name__ == "__main__":
    requests.packages.urllib3.disable_warnings()
    process_gtfs()
