package at.ac.tuwien.mogda.willgraph.controller;

import at.ac.tuwien.mogda.willgraph.repository.AmenityRepository;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/amenities")
public class AmenityController {
    private final AmenityRepository amenityRepository;
    public AmenityController(AmenityRepository amenityRepository) {
        this.amenityRepository = amenityRepository;
    }


}
