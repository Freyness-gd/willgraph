package at.ac.tuwien.mogda.willgraph.controller;

import at.ac.tuwien.mogda.willgraph.controller.dto.PoIDistanceDto;
import at.ac.tuwien.mogda.willgraph.controller.dto.PointToPointDistanceDto;
import at.ac.tuwien.mogda.willgraph.controller.dto.WalkingDistanceDto;
import at.ac.tuwien.mogda.willgraph.entity.PointOfInterestEntity;
import at.ac.tuwien.mogda.willgraph.service.PoIService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/poi")
@Slf4j
public class PoIController {
    private final PoIService poiService;

    public PoIController(PoIService poiService) {
        this.poiService = poiService;
    }

    @GetMapping
    List<PointOfInterestEntity> findByType(@RequestParam("type") String type) {
        return poiService.findAllOfType(type);
    }

    @PostMapping
    public PointOfInterestEntity create(@RequestBody PoIDistanceDto poi) {
        return poiService.create(poi);
    }

    @GetMapping("/{poiId}/walking-distance")
    public WalkingDistanceDto calculateWalkingDistance(
        @PathVariable String poiId,
        @RequestParam("lat") Double targetLatitude,
        @RequestParam("lon") Double targetLongitude) {
        return poiService.calculateWalkingDistance(poiId, targetLatitude, targetLongitude);
    }

    @GetMapping("/nearby")
    public ResponseEntity<List<PoIDistanceDto>> findPoIsNearby(@RequestParam double lat, @RequestParam double lng, @RequestParam(defaultValue = "1000.0") double radius) {
        log.info("GET /pois/nearby lat={} lng={} radius={}", lat, lng, radius);
        return ResponseEntity.status(HttpStatus.OK).body(this.poiService.findPoIsNearby(lat, lng, radius));
    }

    @GetMapping("/distance")
    public PointToPointDistanceDto calculateDistanceBetweenPoints(
        @RequestParam("fromLat") Double fromLatitude,
        @RequestParam("fromLon") Double fromLongitude,
        @RequestParam("toLat") Double toLatitude,
        @RequestParam("toLon") Double toLongitude) {
        return poiService.calculateDistanceBetweenPoints(fromLatitude, fromLongitude, toLatitude, toLongitude);
    }
}
