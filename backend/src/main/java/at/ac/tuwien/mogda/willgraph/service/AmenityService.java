package at.ac.tuwien.mogda.willgraph.service;

import at.ac.tuwien.mogda.willgraph.controller.dto.AmenityOverviewDto;
import at.ac.tuwien.mogda.willgraph.entity.PointOfInterestEntity;

import java.util.List;
import java.util.Set;

public interface AmenityService {

    /**
     * Get all supported amenity types for students/early professionals
     */
    Set<String> getSupportedAmenityTypes();


    /**
     * Fetch amenities within a bounding box and group them by type
     *
     * @return All PointsOfInterest within the bounding Box that fit our supported types
     * @params bounding box of the area to fetch amenities from
     */

    List<PointOfInterestEntity> importAmenityNodes(Double minLat, Double minLon, Double maxLat, Double maxLon);

    List<AmenityOverviewDto> findAmenityOverview(double lat, double lng, double radius);
}
