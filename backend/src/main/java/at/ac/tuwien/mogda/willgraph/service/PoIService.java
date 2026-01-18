package at.ac.tuwien.mogda.willgraph.service;

import at.ac.tuwien.mogda.willgraph.controller.dto.PoIDistanceDto;
import at.ac.tuwien.mogda.willgraph.controller.dto.PointToPointDistanceDto;
import at.ac.tuwien.mogda.willgraph.controller.dto.TransportPathDto;
import at.ac.tuwien.mogda.willgraph.controller.dto.WalkingDistanceDto;
import at.ac.tuwien.mogda.willgraph.entity.PointOfInterestEntity;
import at.ac.tuwien.mogda.willgraph.exception.NotFoundException;

import java.util.List;

public interface PoIService {

    List<PointOfInterestEntity> findAllOfType(String type);

    PointOfInterestEntity create(PoIDistanceDto poi);

    WalkingDistanceDto calculateWalkingDistance(String poiId, Double targetLatitude, Double targetLongitude);

    PointToPointDistanceDto calculateDistanceBetweenPoints(Double fromLat, Double fromLon, Double toLat, Double toLon);

    List<PoIDistanceDto> findPoIsNearby(double lat, double lng, double radius);

    TransportPathDto calculateTransportPath(Double fromLat, Double fromLon, Double toLat, Double toLon, double maxWalkDistance) throws NotFoundException;
}
