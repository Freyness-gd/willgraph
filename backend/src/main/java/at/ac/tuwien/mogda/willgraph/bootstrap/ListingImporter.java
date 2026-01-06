package at.ac.tuwien.mogda.willgraph.bootstrap;

import at.ac.tuwien.mogda.willgraph.entity.AddressEntity;
import at.ac.tuwien.mogda.willgraph.entity.ListingEntity;
import at.ac.tuwien.mogda.willgraph.entity.RegionEntity;
import at.ac.tuwien.mogda.willgraph.repository.AddressRepository;
import at.ac.tuwien.mogda.willgraph.repository.ListingRepository;
import at.ac.tuwien.mogda.willgraph.repository.RegionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
@Order(2)
@RequiredArgsConstructor
@Slf4j
public class ListingImporter implements CommandLineRunner {
    private final ListingRepository listingRepository;
    private final AddressRepository addressRepository;
    private final RegionRepository regionRepository;

    private final Map<Long, AddressEntity> addressCache = new HashMap<>(); // Key: OSM_ID
    private List<RegionEntity> cachedRegions;
    private final GeometryFactory geometryFactory = new GeometryFactory();

    @Override
    public void run(String... args) throws Exception {
        if (listingRepository.count() > 0) {
            log.info("Listings already exist. Skipping import.");
            return;
        }

        log.info("Starting Listing Import...");

        // 1. Pre-load Regions for fast spatial lookup
        // We fetch all regions so we can check "contains(point)" in memory
        this.cachedRegions = regionRepository.findAll();
        log.info("Loaded {} regions into memory for spatial mapping.", cachedRegions.size());

        // 2. Import Willhaben
        importCsv("willhaben_output.csv", "willhaben");

        // 3. Import ImmoScout
        importCsv("immoscout_output.csv", "immoscout");

        log.info("Listing Import Finished.");
    }

    private void importCsv(String filename, String source) {
        try (Reader reader = new InputStreamReader(new ClassPathResource(filename).getInputStream(), StandardCharsets.UTF_8)) {

            // AllowDuplicateHeaderNames is required because your Willhaben CSV has 'url' twice
            CSVFormat format = CSVFormat.DEFAULT.builder()
                    .setHeader()
                    .setSkipHeaderRecord(true)
                    .setIgnoreHeaderCase(true)
                    .setTrim(true)
                    .setAllowDuplicateHeaderNames(true)
                    .build();

            try (CSVParser parser = new CSVParser(reader, format)) {
                int count = 0;
                for (CSVRecord record : parser) {
                    processRow(record, source);
                    count++;
                }
                log.info("Imported {} listings from {}", count, source);
            }
        } catch (Exception e) {
            log.error("Failed to import {}", filename, e);
        }
    }

    private void processRow(CSVRecord record, String source) {
        // --- 1. Extract Common Data ---
        String externalUrl = record.get("url"); // Gets the first occurrence
        String title = record.get("title");

        // Price & Size (Handle potential empty strings safely)
        Double price = parseDouble(record, "price_eur");
        Double size = parseDouble(record, "size_m2");
        Integer rooms = parseInteger(record, source.equals("immoscout") ? "rooms" : "raw_rooms");

        // Geo Data
        Long osmId = parseLong(record, "osm_id");
        Double lat = parseDouble(record, "lat");
        Double lon = parseDouble(record, "lon");
        String locationStr = record.get("location");

        if (lat == null || lon == null) {
            log.warn("Skipping row without coordinates: {}", title);
            return;
        }

        // --- 2. Resolve Address (Deduplication) ---
        // We try to find the address in Cache -> Then DB -> Then Create New
        AddressEntity address = resolveAddress(osmId, lat, lon, locationStr);

        // --- 3. Create Listing ---
        ListingEntity listing = ListingEntity.builder()
                .externalUrl(externalUrl)
                .title(title)
                .price(price)
                .livingArea(size)
                .roomCount(rooms) // Simple mapping, you can add regex for "2 Zimmer" vs "2" later
                .source(source)
                .timestampFound(parseDate(record, source))
                .address(address) // Link to the address node
                .build();

        listingRepository.save(listing);
    }

    private AddressEntity resolveAddress(Long osmId, Double lat, Double lon, String rawAddress) {
        // A. Check Cache (Fastest)
        if (osmId != null && addressCache.containsKey(osmId)) {
            return addressCache.get(osmId);
        }

        // B. Check Database (If we restarted the script but cache is empty)
        // If no OSM_ID, we can't reliably dedup against DB easily without spatial query,
        // so for this script we assume OSM_ID is the key.
        if (osmId != null) {
            Optional<AddressEntity> existing = addressRepository.findByOsmId(osmId);
            if (existing.isPresent()) {
                addressCache.put(osmId, existing.get());
                return existing.get();
            }
        }

        // C. Create New Address
        AddressEntity newAddress = AddressEntity.builder()
                .fullAddressString(rawAddress)
                .osmId(osmId)
                .build();
        newAddress.setLocation(lat, lon);

        // D. Link to Region (Spatial Geometry Check)
        // We check which Region polygon contains this Point
        RegionEntity region = findRegionForPoint(lat, lon);
        if (region != null) {
            newAddress.setRegion(region);
        } else {
            log.warn("Address at {},{} is not inside any known Region polygon.", lat, lon);
        }

        // E. Save and Cache
        AddressEntity saved = addressRepository.save(newAddress);
        if (osmId != null) {
            addressCache.put(osmId, saved);
        }
        return saved;
    }

    private RegionEntity findRegionForPoint(Double lat, Double lon) {
        // Create JTS Point
        org.locationtech.jts.geom.Point point = geometryFactory.createPoint(new Coordinate(lon, lat));

        // Iterate cached regions (Fast in memory check)
        // Since we stored the Geometry in the Entity in the previous script, we can access it here.
        // NOTE: Ensure RegionEntity.getGeometry() returns the JTS Geometry object.
        for (RegionEntity region : cachedRegions) {
            if (region.getGeometry() != null && region.getGeometry().contains(point)) {
                return region;
            }
        }
        return null;
    }

    // --- Helper Parsers ---

    private Double parseDouble(CSVRecord record, String col) {
        try {
            String val = record.get(col);
            return (val == null || val.isBlank()) ? null : Double.parseDouble(val);
        } catch (Exception e) {
            return null;
        }
    }

    private Integer parseInteger(CSVRecord record, String col) {
        try {
            String val = record.get(col);
            // Handle "3" or "3 Zimmer" simply
            val = val.replaceAll("[^0-9]", "");
            return (val.isBlank()) ? null : Integer.parseInt(val);
        } catch (Exception e) {
            return null;
        }
    }

    private Long parseLong(CSVRecord record, String col) {
        try {
            String val = record.get(col);
            return (val == null || val.isBlank()) ? null : Long.parseLong(val.split("\\.")[0]); // Handle "123.0"
        } catch (Exception e) {
            return null;
        }
    }

    private LocalDateTime parseDate(CSVRecord record, String source) {
        if ("immoscout".equals(source)) {
            try {
                // Format: 2026-01-04 19:18:19.080029
                String dateStr = record.get("scraped_at");
                // Simple parser - you might need a customized DateTimeFormatter for nanoseconds
                return LocalDateTime.parse(dateStr.replace(" ", "T"));
            } catch (Exception e) {
                return LocalDateTime.now();
            }
        }
        return LocalDateTime.now(); // Willhaben output didn't have a timestamp column in the snippet
    }

}
