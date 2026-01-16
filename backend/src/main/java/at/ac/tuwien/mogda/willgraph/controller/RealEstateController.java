package at.ac.tuwien.mogda.willgraph.controller;

import at.ac.tuwien.mogda.willgraph.controller.dto.ListingSearchFilterDto;
import at.ac.tuwien.mogda.willgraph.controller.dto.RealEstateDto;
import at.ac.tuwien.mogda.willgraph.controller.dto.RealEstateWithScoreDto;
import at.ac.tuwien.mogda.willgraph.controller.dto.StationDistanceDto;
import at.ac.tuwien.mogda.willgraph.exception.NotFoundException;
import at.ac.tuwien.mogda.willgraph.service.RealEstateService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/estate")
@CrossOrigin(origins = "*", allowedHeaders = "*")
@Slf4j
public class RealEstateController {

    private final RealEstateService realEstateService;

    public RealEstateController(RealEstateService realEstateService) {
        this.realEstateService = realEstateService;
    }

    @GetMapping("/{id}")
    public ResponseEntity<RealEstateDto> findById(@PathVariable String id) {
        log.info("GET /api/estate/{}", id);
        if (id == null || id.isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
        try {
            return ResponseEntity.status(HttpStatus.OK).body(realEstateService.findById(id));
        } catch (NotFoundException _) {
            return ResponseEntity.notFound().build();
        }
    }


    @GetMapping()
    public ResponseEntity<List<RealEstateDto>> findInsideRegion(@RequestParam(required = false) String region, @RequestParam(required = false) String iso) {
        log.info("GET /api/estate?region={}&iso={}", region, iso);
        if (region == null && iso == null) {
            return ResponseEntity.status(HttpStatus.OK).body(realEstateService.findAll());
        }
        try {
            return ResponseEntity.status(HttpStatus.OK).body(realEstateService.findRealEstatesInRegion(region, iso));
        } catch (NotFoundException _) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    //TODO: Check which URI to use
    @PostMapping("/search")
    public ResponseEntity<List<RealEstateWithScoreDto>> searchWithFilters(@RequestBody ListingSearchFilterDto request) {
        log.info("POST /api/estate/search");
        log.info("Search body = {}", request);
        try {
            return ResponseEntity.status(HttpStatus.OK).body(realEstateService.searchWithFilters(request));
        } catch (NotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @GetMapping("/{id}/transport")
    public ResponseEntity<List<StationDistanceDto>> findStationsNearby(@PathVariable String id) {
        log.info("GET /api/estate/{}/transport", id);
        try {
            return ResponseEntity.status(HttpStatus.OK).body(this.realEstateService.findStationsNearby(id));
        } catch (NotFoundException _) {
            return ResponseEntity.notFound().build();
        }
    }
}
