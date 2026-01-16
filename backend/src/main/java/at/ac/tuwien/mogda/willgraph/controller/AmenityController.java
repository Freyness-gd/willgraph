package at.ac.tuwien.mogda.willgraph.controller;

import at.ac.tuwien.mogda.willgraph.controller.dto.AmenityOverviewDto;
import at.ac.tuwien.mogda.willgraph.service.AmenityService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/amenities")
@Slf4j
public class AmenityController {
    private final AmenityService amenityService;

    public AmenityController(AmenityService amenityService) {
        this.amenityService = amenityService;
    }

    @GetMapping("/nearby")
    public ResponseEntity<List<AmenityOverviewDto>> findAmenityOverview(@RequestParam double lat, @RequestParam double lng, @RequestParam(defaultValue = "1000.0") double radius) {
        log.info("GET /amenities/nearby lat={} lng={} radius={}", lat, lng, radius);
        return ResponseEntity.status(HttpStatus.OK).body(this.amenityService.findAmenityOverview(lat, lng, radius));
    }
}
