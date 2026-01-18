package at.ac.tuwien.mogda.willgraph.service.impl;

import at.ac.tuwien.mogda.willgraph.controller.dto.PoIDistanceDto;
import at.ac.tuwien.mogda.willgraph.controller.dto.PointToPointDistanceDto;
import at.ac.tuwien.mogda.willgraph.controller.dto.TransportPathDto;
import at.ac.tuwien.mogda.willgraph.controller.dto.WalkingDistanceDto;
import at.ac.tuwien.mogda.willgraph.entity.AmenityTypeEntity;
import at.ac.tuwien.mogda.willgraph.entity.PointOfInterestEntity;
import at.ac.tuwien.mogda.willgraph.exception.NotFoundException;
import at.ac.tuwien.mogda.willgraph.repository.AddressRepository;
import at.ac.tuwien.mogda.willgraph.repository.AmenityTypeRepository;
import at.ac.tuwien.mogda.willgraph.repository.PoIRepository;
import at.ac.tuwien.mogda.willgraph.service.PoIService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.neo4j.types.GeographicPoint2d;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PoIServiceImpl implements PoIService {

    private static final double WALKING_SPEED_METERS_PER_MINUTE = 80.0;

    private final PoIRepository poiRepository;
    private final AddressRepository addressRepository;
    private final AmenityTypeRepository amenityTypeRepository;

    @Override
    public List<PointOfInterestEntity> findAllOfType(String type) {
        AmenityTypeEntity typeEntity = this.amenityTypeRepository.findById(type).orElse(null);
        if (typeEntity == null) {
            return Collections.emptyList();
        }
        return this.poiRepository.findAllByType(typeEntity);
    }

    @Override
    public PointOfInterestEntity create(PoIDistanceDto poi) {
        //TODO: Validation && Logger

//        AddressCreateDto poiAddress = poi.getAddress();
//        PointOfInterestEntity poiEntity = PointOfInterestEntity.builder()
//            .score(poi.getScore())
//            .type(poi.getType())
//            .build();
//        if (poiAddress != null) {
//            AddressEntity address = AddressEntity.builder()
//                .city(poiAddress.getCity())
//                .countryCode(poiAddress.getCountryCode())
//                .houseNumber(poiAddress.getHouseNumber())
//                .street(poiAddress.getStreet())
//                .latitude(poiAddress.getLatitude())
//                .longitude(poiAddress.getLongitude())
//                .postalCode(poiAddress.getPostalCode())
//                .build();
//            poiEntity.setAddress(address);
//        }
//        return this.poiRepository.save(poiEntity);
        return null;
    }

    @Override
    public WalkingDistanceDto calculateWalkingDistance(String poiId, Double targetLatitude, Double targetLongitude) {
        PointOfInterestEntity poi = poiRepository.findById(poiId)
            .orElseThrow(() -> new IllegalArgumentException("POI not found with id: " + poiId));

        GeographicPoint2d poiLocation = poi.getLocation();
        if (poiLocation == null) {
            throw new IllegalStateException("POI has no location defined");
        }

        double distanceInMeters = calculateHaversineDistance(
            poiLocation.getLatitude(), poiLocation.getLongitude(),
            targetLatitude, targetLongitude
        );

        double walkingDurationInMinutes = distanceInMeters / WALKING_SPEED_METERS_PER_MINUTE;

        return WalkingDistanceDto.builder()
            .poiId(poiId)
            .poiName(poi.getName())
            .targetLatitude(targetLatitude)
            .targetLongitude(targetLongitude)
            .distanceInMeters(distanceInMeters)
            .walkingDurationInMinutes(walkingDurationInMinutes)
            .build();
    }

    @Override
    public PointToPointDistanceDto calculateDistanceBetweenPoints(Double fromLat, Double fromLon, Double toLat, Double toLon) {
        double distanceInMeters = calculateHaversineDistance(fromLat, fromLon, toLat, toLon);
        double walkingDurationInMinutes = distanceInMeters / WALKING_SPEED_METERS_PER_MINUTE;

        return PointToPointDistanceDto.builder()
            .fromLatitude(fromLat)
            .fromLongitude(fromLon)
            .toLatitude(toLat)
            .toLongitude(toLon)
            .distanceInMeters(distanceInMeters)
            .walkingDurationInMinutes(walkingDurationInMinutes)
            .build();
    }

    @Override
    public List<PoIDistanceDto> findPoIsNearby(double lat, double lng, double radius) {
        return this.poiRepository.findPoIsNearby(lat, lng, radius);
    }

    @Override
    public TransportPathDto calculateTransportPath(Double fromLat, Double fromLon, Double toLat, Double toLon, double maxWalkDistance) throws NotFoundException {
        return poiRepository.findShortestTransportPath(fromLat, fromLon, toLat, toLon, maxWalkDistance)
            .orElseThrow(() -> new NotFoundException(
                "No transport path found (locations might be too far from a station or not connected)"
            ));
    }

    /**
     * Calculate distance between two points using Haversine formula.
     *
     * @param lat1 Latitude of point 1
     * @param lon1 Longitude of point 1
     * @param lat2 Latitude of point 2
     * @param lon2 Longitude of point 2
     * @return Distance in meters
     */
    private double calculateHaversineDistance(double lat1, double lon1, double lat2, double lon2) {
        final double EARTH_RADIUS_METERS = 6371000.0;

        double lat1Rad = Math.toRadians(lat1);
        double lat2Rad = Math.toRadians(lat2);
        double deltaLat = Math.toRadians(lat2 - lat1);
        double deltaLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2)
            + Math.cos(lat1Rad) * Math.cos(lat2Rad)
            * Math.sin(deltaLon / 2) * Math.sin(deltaLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return EARTH_RADIUS_METERS * c;
    }
}
