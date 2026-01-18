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
import logging
import sys
import json
from datetime import datetime
from dotenv import load_dotenv

load_dotenv()
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s [%(levelname)s] %(message)s',
    handlers=[logging.StreamHandler(sys.stdout)]
)
logger = logging.getLogger(__name__)

BASE_DIR = './gtfs_data'
OUTPUT_NODES = os.path.join(BASE_DIR, 'transport_nodes.csv')
OUTPUT_EDGES = os.path.join(BASE_DIR, 'transport_edges.csv')
STATE_FILE = os.path.join(BASE_DIR, 'download_state.json')

DBP_USERNAME = os.getenv("DBP_USERNAME", "")
DBP_PASSWORD = os.getenv("DBP_PASSWORD", "")

ONEDRIVE_FALLBACK_LINK = os.getenv("ONEDRIVE_LINK", "https://tuwienacat-my.sharepoint.com/:u:/g/personal/e12122386_student_tuwien_ac_at/IQD89Okos9TBRqNTGmMl0f04Ae3K7oxHHBK59MOKDI0dj58?e=63fLlP&download=1")
TOKEN_URL = "https://user.mobilitaetsverbuende.at/auth/realms/dbp-public/protocol/openid-connect/token"
API_BASE_URL = "https://data.mobilitaetsverbuende.at/api/public/v1/data-sets"
CLIENT_ID = "dbp-public-ui"

AUSTRIAN_STATES = [
  "Wien", "Vienna",
  "Niederösterreich", "Oberösterreich", "Salzburg",
  "Tirol", "Vorarlberg", "Kärnten", "Steiermark", "Burgenland",
  "Vienna", "Upper Austria", "Lower Austria", "Carinthia", "Styria", "Tyrol"
]
class DBPClient:
    def __init__(self, username, password):
        self.username = username
        self.password = password
        self.token = None
        self.state = self.load_state()
        self.updates_count = 0

    def load_state(self):
        """Loads the local state of downloaded files."""
        if os.path.exists(STATE_FILE):
            try:
                with open(STATE_FILE, 'r') as f:
                    return json.load(f)
            except Exception:
                return {}
        return {}

    def save_state(self):
        """Saves the current state to disk."""
        os.makedirs(BASE_DIR, exist_ok=True)
        with open(STATE_FILE, 'w') as f:
            json.dump(self.state, f, indent=2)

    def authenticate(self):
        logger.info("Authenticating with DBP...")
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
            logger.info("Authentication successful.")
        except Exception as e:
            logger.error(f"Authentication failed: {e}")
            sys.exit(1)

    def get_headers(self):
        return {
            'Authorization': f'Bearer {self.token}',
            'Accept': 'application/json'
        }

    def sync_regional_gtfs(self, output_base_dir):
        if not self.token: return

        url = f"{API_BASE_URL}?tagIds=20&tagFilterModeInclusive=true"
        logger.info(f"Fetching dataset list from {url}...")
        try:
            response = requests.get(url, headers=self.get_headers(), verify=False)
            datasets = response.json()
        except Exception as e:
            logger.error(f"Error listing datasets: {e}")
            return

        current_year = str(datetime.now().year)

        for ds in datasets:
            ds_name = ds.get('nameDe', '') or ds.get('name', '')
            ds_id = str(ds.get('id'))

            #Exclusion
            if "flex" in ds_name.lower():
                logger.info(f"Skipping (Excluded Name): {ds_name}")
                continue

            if "in sieben teilen" in ds_name.lower():
                logger.info(f"Skipping (Excluded Split): {ds_name}")
                continue

            tags = ds.get('tags', [])
            tag_values = [t.get('valueDe', '') for t in tags] + [t.get('valueEn', '') for t in tags]

            is_regional = any(state.lower() in ds_name.lower() for state in AUSTRIAN_STATES) or \
                          any(state in tag_val for state in AUSTRIAN_STATES for tag_val in tag_values)
            if is_regional:
                active_versions = ds.get('activeVersions', [])
                if not active_versions:
                    logger.warning(f"  Skipping {ds_name} (No active version found)")
                    continue
                latest_version_obj = active_versions[0].get('dataSetVersion', {})
                remote_version_id = str(latest_version_obj.get('id'))
                if not remote_version_id:
                    logger.warning(f"  Skipping {ds_name} (Missing version ID)")
                    continue
                self.download_and_extract(ds_id, ds_name, current_year, output_base_dir, remote_version_id)

        logger.info(f"Sync finished. Total updates applied: {self.updates_count}")
        return self.updates_count

    def download_and_extract(self, ds_id, dataset_name, current_year, output_base_dir, remote_version_id):
        clean_name = re.sub(r'[^a-zA-Z0-9]', '_', dataset_name)
        target_dir = os.path.join(output_base_dir, f"{ds_id}_{clean_name}")

        local_version_id = self.state.get(ds_id)
        if local_version_id == remote_version_id and os.path.exists(target_dir):
            logger.info(f"  ID {ds_id}: Up to date (Version {remote_version_id}). Skipping.")
            return

        logger.info(f"  ID {ds_id}: Update found (v{local_version_id} -> v{remote_version_id}). Downloading...")
        url = f"{API_BASE_URL}/{ds_id}/{current_year}/file"
        temp_zip_path = os.path.join(output_base_dir, "temp_gtfs.zip")
        os.makedirs(output_base_dir, exist_ok=True)
        try:
            headers = self.get_headers()
            headers['Accept'] = 'application/zip'
            logger.info(f"  Downloading ID {ds_id} (Streaming to disk)...")
            with requests.get(url, headers=headers, stream=True, verify=False, timeout=60) as response:
                response.raise_for_status()
                with open(temp_zip_path, 'wb') as f:
                    for chunk in response.iter_content(chunk_size=8192):
                        if chunk:
                            f.write(chunk)

            if os.path.exists(target_dir):
                shutil.rmtree(target_dir)
            os.makedirs(target_dir, exist_ok=True)

            logger.info(f"  Extracting {temp_zip_path}...")
            with zipfile.ZipFile(temp_zip_path, 'r') as z:
                z.extractall(target_dir)

            os.remove(temp_zip_path)
            self.state[ds_id] = remote_version_id
            self.save_state()
            self.updates_count += 1
        except Exception as e:
            logger.error(f"  Failed to download ID {ds_id}: {e}")
            if os.path.exists(temp_zip_path):
                os.remove(temp_zip_path)

def fetch_data_from_api():
    if not DBP_USERNAME or "EXAMPLE.COM" in DBP_USERNAME:
        logger.warning("Skipping API download (Credentials not set).")
        raise ValueError("Credentials not set or default.")

    client = DBPClient(DBP_USERNAME, DBP_PASSWORD)
    client.authenticate()

    if not client.token:
        sys.exit(1)

    updates_found = client.sync_regional_gtfs(BASE_DIR)
    return updates_found

def fetch_from_onedrive():
    """Fallback: Downloads a complete ZIP of the directory structure from OneDrive."""
    logger.info("--- INITIATING FALLBACK DOWNLOAD FROM ONEDRIVE ---")

    if not ONEDRIVE_FALLBACK_LINK or "YOUR_ID" in ONEDRIVE_FALLBACK_LINK:
        logger.error("OneDrive link is not configured properly.")
        return False

    temp_fallback_zip = os.path.join(BASE_DIR, "fallback_dump.zip")
    os.makedirs(BASE_DIR, exist_ok=True)

    try:
        logger.info(f"Downloading from OneDrive: {ONEDRIVE_FALLBACK_LINK}...")
        with requests.get(ONEDRIVE_FALLBACK_LINK, stream=True, verify=False) as r:
            r.raise_for_status()
            total_size = int(r.headers.get('content-length', 0))
            downloaded = 0

            with open(temp_fallback_zip, 'wb') as f:
                for chunk in r.iter_content(chunk_size=32*1024):
                    if chunk:
                        f.write(chunk)
                        downloaded += len(chunk)
                        if total_size > 0 and downloaded % (1024*1024*10) == 0:
                            print(f"\rDownloaded {downloaded//(1024*1024)} MB...", end="")
        print("") # Newline
        logger.info("Download complete. Extracting...")

        with zipfile.ZipFile(temp_fallback_zip, 'r') as z:
            z.extractall(BASE_DIR)

        logger.info("Extraction complete.")
        os.remove(temp_fallback_zip)
        return True

    except Exception as e:
        logger.error(f"OneDrive fallback failed: {e}")
        if os.path.exists(temp_fallback_zip):
            os.remove(temp_fallback_zip)
        return False

def parse_gtfs_time(time_str):
    if pd.isna(time_str):
        return None
    try:
        h, m, s = map(int, time_str.split(':'))
        return h * 60 + m + s / 60.0  # Returns minutes from midnight
    except:
        return None

def process_gtfs():
    updates_found = 0
    used_fallback = False
    try:
        updates_found = fetch_data_from_api()
    except Exception as e:
        logger.warning(f"API Sync failed or skipped: {e}")
        if os.path.exists(OUTPUT_NODES) and os.path.exists(OUTPUT_EDGES):
            logger.info("API failed, but files exists locally.")
            logger.info("Skipping OneDrive download. Using existing processed data.")
            sys.exit(0)
        elif local_raw_data_exists():
            logger.info("API failed, but raw GTFS data found in folders.")
            logger.info("Skipping OneDrive download. Will regenerate CSVs from local files.")
            updates_found = 1
        else:
            logger.info("Local data missing. Attempting switch to OneDrive fallback...")
            success = fetch_from_onedrive()
            if success:
                updates_found = 1
                used_fallback = True
            else:
                logger.error("Both API and Fallback failed.")
                sys.exit(1)

    files_exist = os.path.exists(OUTPUT_NODES) and os.path.exists(OUTPUT_EDGES)
    if updates_found == 0 and files_exist and not used_fallback:
        logger.info("No new datasets found and output files exist. Skipping processing.")
        sys.exit(0)
    if updates_found == 0 and not files_exist:
        logger.info("No updates found, but output files are missing. Regenerating from local GTFS...")
    else:
        logger.info("Updates detected.")

    logger.info("Loading GTFS data from disk...")
    stop_files = glob.glob(os.path.join(BASE_DIR, '**', 'stops.txt'), recursive=True)
    time_files = glob.glob(os.path.join(BASE_DIR, '**', 'stop_times.txt'), recursive=True)

    if not stop_files:
        logger.error(f"No stops.txt found in subdirectories of {BASE_DIR}")
        sys.exit(1)

    logger.info(f"Found {len(stop_files)} GTFS directories.")

    logger.info("Loading stops from all sources...")
    stops_cols = ['stop_id', 'stop_name', 'stop_lat', 'stop_lon']
    stops_df_list = []

    for f in stop_files:
        logger.debug(f"  Reading {f}...")
        try:
            df = pd.read_csv(f, usecols=lambda c: c in stops_cols, dtype={'stop_id': str})
            stops_df_list.append(df)
        except Exception as e:
            logger.warning(f"  Could not read {f}: {e}")

    if not stops_df_list:
        logger.error("No valid stops data found.")
        sys.exit(1)

    all_stops = pd.concat(stops_df_list, ignore_index=True)
    all_stops.drop_duplicates(subset=['stop_id'], inplace=True)
    logger.info(f"Total unique stops: {len(all_stops)}")

    logger.info("Processing stop_times...")
    time_cols = ['trip_id', 'stop_id', 'arrival_time', 'departure_time', 'stop_sequence']

    all_edges = []

    for f in time_files:
        dataset_name = os.path.basename(os.path.dirname(f))
        logger.info(f"  Processing routes in {dataset_name}...")

        try:
            df = pd.read_csv(f, usecols=lambda c: c in time_cols,
                             dtype={'trip_id': str, 'stop_id': str, 'stop_sequence': 'int32'})
            if df.empty: continue

            df.sort_values(by=['trip_id', 'stop_sequence'], inplace=True)

            df['next_stop_id'] = df['stop_id'].shift(-1)
            df['next_trip_id'] = df['trip_id'].shift(-1)
            df['next_arrival_time'] = df['arrival_time'].shift(-1)

            valid = df[df['trip_id'] == df['next_trip_id']].copy()
            del df  # Free RAM

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
            logger.warning(f"  Skipping {f} due to error: {e}")

    if not all_edges:
        logger.error("No edges created.")
        sys.exit(1)

    logger.info("Merging network...")
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

    logger.info(f"Exporting {len(all_stops)} nodes and {len(final_edges)} edges...")
    all_stops.to_csv(OUTPUT_NODES, index=False)
    final_edges.to_csv(OUTPUT_EDGES, index=False)
    logger.info("Done!")

if __name__ == "__main__":
    requests.packages.urllib3.disable_warnings()
    process_gtfs()
