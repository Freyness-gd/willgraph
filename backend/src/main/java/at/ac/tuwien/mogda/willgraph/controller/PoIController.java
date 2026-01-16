package at.ac.tuwien.mogda.willgraph.controller;

import at.ac.tuwien.mogda.willgraph.controller.dto.PoIDto;
import at.ac.tuwien.mogda.willgraph.controller.dto.PointToPointDistanceDto;
import at.ac.tuwien.mogda.willgraph.controller.dto.WalkingDistanceDto;
import at.ac.tuwien.mogda.willgraph.entity.PointOfInterestEntity;
import at.ac.tuwien.mogda.willgraph.service.PoIService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/pois")
public class PoIController {
    private final PoIService poiService;

    public PoIController(PoIService poiService) {
        this.poiService = poiService;
    }

    @GetMapping
    List<PointOfInterestEntity> findByType(@RequestParam("type") String type){
        return poiService.findAllOfType(type);
    }

    @PostMapping
    public PointOfInterestEntity create(@RequestBody PoIDto poi){
        return poiService.create(poi);
    }

    @GetMapping("/{poiId}/walking-distance")
    public WalkingDistanceDto calculateWalkingDistance(
            @PathVariable String poiId,
            @RequestParam("lat") Double targetLatitude,
            @RequestParam("lon") Double targetLongitude) {
        return poiService.calculateWalkingDistance(poiId, targetLatitude, targetLongitude);
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
