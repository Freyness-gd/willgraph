package at.ac.tuwien.mogda.willgraph.service.impl;

import at.ac.tuwien.mogda.willgraph.repository.AmenityRepository;
import at.ac.tuwien.mogda.willgraph.service.AmenityService;
import org.springframework.stereotype.Service;

@Service
public class AmenityServiceImpl implements AmenityService {
    private AmenityRepository amenityRepository;
    public AmenityServiceImpl(AmenityRepository amenityRepository) {
        this.amenityRepository = amenityRepository;
    }


}
