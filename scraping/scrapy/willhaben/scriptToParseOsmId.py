import csv
import re
import time
import urllib.parse
import requests
import sys

INPUT_CSV = "willhaben_output.csv"
OUTPUT_CSV = "output_with_osm.csv"
NOMINATIM_URL = "https://nominatim.openstreetmap.org/search"
#TODO restrict to vienna only and make async
headers = {
    "User-Agent": "LocationLookupScript/1.0"  # Nominatim requires a User-Agent
}

# Cache for location queries
location_cache = {}

def query_location(location):
    # Check cache first
    if location in location_cache:
        print(f"Cache hit for location: {location}")
        sys.stdout.flush()
        return location_cache[location]
    
    #url_encoded_location = urllib.parse.quote(location)
    params = {
        "q": location,
        "email": "e12121286@student.tuwien.ac.at",
        "limit": 1,
        "format": "jsonv2"
    }
    r = requests.get(NOMINATIM_URL, params=params, headers=headers, timeout=10)
    #get url and status code for logging
    print(f"Requested URL: {r.url}")
    #print(f"Queried Nominatim for location: {location}, Status Code: {r.status_code}")
    #print(f"Response Content: {r.text}")
    sys.stdout.flush()
    if r.status_code != 200:
        location_cache[location] = None
        return None
    data = r.json()
    if not data:
        location_cache[location] = None
        return None
    result = data[0]
    cached_result = {
        "osm_id": result.get("osm_id"),
        "lat": result.get("lat"),
        "lon": result.get("lon")
    }
    # Store in cache
    location_cache[location] = cached_result
    return cached_result


def parseAndSaveNewFromRawLocation(location):
    with open(INPUT_CSV, newline="", encoding="utf-8") as infile, \
            open(OUTPUT_CSV, "w", newline="", encoding="utf-8") as outfile:
        reader = csv.DictReader(infile)
    fieldnames = reader.fieldnames + ["url", "osm_id", "lat", "lon"]
    writer = csv.DictWriter(outfile, fieldnames=fieldnames)
    writer.writeheader()

    for row in reader:
        location = row.get("location")
    url = row.get("url")
    if location:
    # remove following regex from location if present
        location = location.replace('"', '')
        location = re.sub(r"\d+\. Bezirk,\s*", "", location)

    # sleep a bit to respect Nominatim usage policy
    time.sleep(1)
    info = query_location(url, location)
    if info:
        row.update(info)
    else:
        row.update({"url": url, "osm_id": None, "lat": None, "lon": None})
    writer.writerow(row)


