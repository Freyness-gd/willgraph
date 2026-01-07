package at.ac.tuwien.mogda.willgraph.controller;

import at.ac.tuwien.mogda.willgraph.controller.dto.RegionDto;
import at.ac.tuwien.mogda.willgraph.exception.NotFoundException;
import at.ac.tuwien.mogda.willgraph.service.RegionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/regions")
@RequiredArgsConstructor
@Slf4j
public class RegionController {
    private final RegionService regionService;

    @GetMapping
    public ResponseEntity<List<RegionDto>> getRegions(@RequestParam(required = false) String q,
                                                      @RequestParam(defaultValue = "10") Integer limit) {
        log.info("GET /regions query={} limit={}", q, limit);
        return ResponseEntity.status(HttpStatus.OK).body(regionService.searchRegions(q, limit));
    }


    @GetMapping("/{id}")
    public ResponseEntity<RegionDto> getRegionById(@PathVariable Long id) {
        log.info("GET /regions/{}", id);
        try {
            return ResponseEntity.status(HttpStatus.OK).body(regionService.getRegionById(id));
        } catch (NotFoundException _) {
            return ResponseEntity.notFound().build();
        }
    }
}
