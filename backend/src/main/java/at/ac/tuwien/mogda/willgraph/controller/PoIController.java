package at.ac.tuwien.mogda.willgraph.controller;

import at.ac.tuwien.mogda.willgraph.controller.dto.PoIDto;
import at.ac.tuwien.mogda.willgraph.entity.PointOfInterestEntity;
import at.ac.tuwien.mogda.willgraph.service.PoIService;
import jakarta.annotation.security.RolesAllowed;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

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

}
