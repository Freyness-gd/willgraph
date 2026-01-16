package at.ac.tuwien.mogda.willgraph.bootstrap;

import at.ac.tuwien.mogda.willgraph.config.AmenitySearchConfig;
import at.ac.tuwien.mogda.willgraph.entity.PointOfInterestEntity;
import at.ac.tuwien.mogda.willgraph.service.AmenityService;
import at.ac.tuwien.mogda.willgraph.service.OverpassApiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import java.util.List;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(3)
@RequiredArgsConstructor
@Slf4j
public class AmenityImporter implements CommandLineRunner {
  private final AmenityService amenityService;
  private final AmenitySearchConfig amenitySearchConfig;
  private final OverpassApiService overpassApiService;

  @Override
  public void run(String... args) throws Exception {
    if (!amenitySearchConfig.getAutoFetchOnStartup()) {
      log.info("Amenity pre-fetching disabled in configuration.");
      return;
    }

    AmenitySearchConfig.BoundingBox bbox = amenitySearchConfig.getBoundingBox();

    log.info("Starting amenity pre-fetching for bounding box ({}, {}) to ({}, {})",
        bbox.getMinLat(), bbox.getMinLon(), bbox.getMaxLat(), bbox.getMaxLon());

    try {
      // Fetch and save all supported amenities
      List<PointOfInterestEntity> response = amenityService.importAmenityNodes(bbox.getMinLat(), bbox.getMinLon(), bbox.getMaxLat(), bbox.getMaxLon());
      log.debug("Fetched {} POIs in bbox {}", response.size(), bbox);

    } catch (Exception e) {
      log.warn("Failed to pre-fetch amenities. Amenity features will work but with delayed first response.", e);
    }
  }

}
