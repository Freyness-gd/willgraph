package at.ac.tuwien.mogda.willgraph.controller;

import at.ac.tuwien.mogda.willgraph.controller.dto.RealEstateDto;
import at.ac.tuwien.mogda.willgraph.exception.NotFoundException;
import at.ac.tuwien.mogda.willgraph.service.RealEstateService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
        if (region == null && iso == null) {
            return ResponseEntity.status(HttpStatus.OK).body(realEstateService.findAll());
        }
        try {
            return ResponseEntity.status(HttpStatus.OK).body(realEstateService.findRealEstatesInRegion(region, iso));
        } catch (NotFoundException _) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

    }
}
