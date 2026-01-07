import csv

import pandas as pd
import numpy as np
import os
import glob

BASE_DIR = './gtfs_data'
OUTPUT_NODES = 'transport_nodes.csv'
OUTPUT_EDGES = 'transport_edges.csv'

def parse_gtfs_time(time_str):
    if pd.isna(time_str):
        return None
    try:
        h, m, s = map(int, time_str.split(':'))
        return h * 60 + m + s / 60.0  # Returns minutes from midnight
    except:
        return None

def process_gtfs():
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

    print("Processing stop_times (this takes memory)...")
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
    process_gtfs()
