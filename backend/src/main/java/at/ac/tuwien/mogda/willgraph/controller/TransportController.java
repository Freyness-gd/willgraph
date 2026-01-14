package at.ac.tuwien.mogda.willgraph.controller;

import at.ac.tuwien.mogda.willgraph.controller.dto.StationDistanceDto;
import at.ac.tuwien.mogda.willgraph.service.TransportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/transport")
@Slf4j
@RequiredArgsConstructor
public class TransportController {

    private final TransportService transportService;

    @GetMapping("/nearby")
    public ResponseEntity<List<StationDistanceDto>> findTransportStationsNearby(@RequestParam double lat, @RequestParam double lng, @RequestParam(defaultValue = "1000.0") double radius) {
        log.info("GET /api/transport/nearby lat={} lng={} radius={}", lat, lng, radius);
        return ResponseEntity.status(HttpStatus.OK).body(this.transportService.findStationsByLocation(lat, lng, radius));


    }


}
