# Transport Importer

The transport importer scripts: gtfs_processor.py and import_transport_to_neo4j.py
download data (currently) from https://data.mobilitaetsverbuende.at/de/data-sets
which is the austrian dataset collection for all stops and stop_times for the whole country.

**Important:** The gtfs_processor needs to authenticate with https://data.mobilitaetsverbuende.at/de/data-sets
therefore you need to provide a .env file in this directory with:

```
DBP_USERNAME=your_email@gmail.com
DBP_PASSWORD=your_password
```

Both can be gained via registering on the site: https://data.mobilitaetsverbuende.at/de

The importer is integrated into the docker-compose inside the backend directory and runs before the backend.