package at.ac.tuwien.mogda.willgraph.service.scheduled;

import at.ac.tuwien.mogda.willgraph.entity.AddressEntity;
import at.ac.tuwien.mogda.willgraph.entity.RegionEntity;
import at.ac.tuwien.mogda.willgraph.repository.AddressRepository;
import at.ac.tuwien.mogda.willgraph.repository.RegionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.springframework.data.neo4j.types.GeographicPoint2d;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class RegionUpdateScheduler {
    private final AddressRepository addressRepository;
    private final RegionRepository regionRepository;
    private final GeometryFactory geometryFactory = new GeometryFactory();

    /**
     * Runs at startup and every 5 minutes to update region assignments for addresses.
     * Uses JTS polygon containment for accurate region mapping.
     */
    @Scheduled(fixedRate = 300000, initialDelay = 0) // 300000ms = 5 minutes
    @Transactional
    public void updateAddressRegions() {
        log.info("Starting scheduled region update for addresses...");

        List<RegionEntity> regions = regionRepository.findAll();
        if (regions.isEmpty()) {
            log.warn("No regions found. Skipping region update.");
            return;
        }

        log.info("Loaded {} regions for spatial mapping.", regions.size());

        // Find all addresses without region assignment or optionally all addresses
        List<AddressEntity> addresses = addressRepository.findAll();
        int updated = 0;
        int skipped = 0;
        int failed = 0;

        for (AddressEntity address : addresses) {
            GeographicPoint2d location = address.getLocation();
            if (location == null) {
                skipped++;
                continue;
            }

            try {
                RegionEntity correctRegion = findRegionForPoint(
                    location.getLatitude(),
                    location.getLongitude(),
                    regions
                );

                RegionEntity currentRegion = address.getRegion();

                // Update if no region or if current region doesn't contain the point
                if (currentRegion == null || !currentRegion.equals(correctRegion)) {
                    if (correctRegion != null) {
                        address.setRegion(correctRegion);
                        addressRepository.save(address);
                        updated++;
                        log.debug("Updated address {} to region {}", 
                            address.getFullAddressString(), correctRegion.getName());
                    } else {
                        log.warn("Address at {},{} is not inside any known Region polygon.",
                            location.getLatitude(), location.getLongitude());
                        // Optionally clear incorrect region assignment
                        if (currentRegion != null) {
                            address.setRegion(null);
                            addressRepository.save(address);
                            updated++;
                        }
                        failed++;
                    }
                }
            } catch (Exception e) {
                log.error("Failed to update region for address {}: {}", 
                    address.getFullAddressString(), e.getMessage());
                failed++;
            }
        }

        log.info("Region update completed. Total: {}, Updated: {}, Skipped (no location): {}, Failed: {}",
            addresses.size(), updated, skipped, failed);
    }

    private RegionEntity findRegionForPoint(Double lat, Double lon, List<RegionEntity> regions) {
        Point point = geometryFactory.createPoint(new Coordinate(lon, lat));

        for (RegionEntity region : regions) {
            if (region.getGeometry() != null && region.getGeometry().contains(point)) {
                return region;
            }
        }
        return null;
    }
}
