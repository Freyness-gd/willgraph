package at.ac.tuwien.mogda.willgraph.service;

import at.ac.tuwien.mogda.willgraph.controller.dto.PoIDto;
import at.ac.tuwien.mogda.willgraph.controller.dto.PointToPointDistanceDto;
import at.ac.tuwien.mogda.willgraph.controller.dto.WalkingDistanceDto;
import at.ac.tuwien.mogda.willgraph.entity.PointOfInterestEntity;

import java.util.List;

public interface PoIService {

    List<PointOfInterestEntity> findAllOfType(String type);

    PointOfInterestEntity create(PoIDto poi);

    WalkingDistanceDto calculateWalkingDistance(String poiId, Double targetLatitude, Double targetLongitude);

    PointToPointDistanceDto calculateDistanceBetweenPoints(Double fromLat, Double fromLon, Double toLat, Double toLon);
}
