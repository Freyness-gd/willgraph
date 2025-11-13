package at.ac.tuwien.mogda.willgraph.service;

import at.ac.tuwien.mogda.willgraph.controller.dto.PoIDto;
import at.ac.tuwien.mogda.willgraph.entity.PointOfInterestEntity;
import reactor.core.publisher.Mono;

import java.util.List;

public interface PoIService {

    List<PointOfInterestEntity> findAllOfType(String type);

    PointOfInterestEntity create(PoIDto poi);
}
