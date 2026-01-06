package at.ac.tuwien.mogda.willgraph.repository;

import at.ac.tuwien.mogda.willgraph.entity.PointOfInterestEntity;
import org.springframework.data.neo4j.repository.Neo4jRepository;

import java.util.List;

public interface PoIRepository extends Neo4jRepository<PointOfInterestEntity, Long> {
    List<PointOfInterestEntity> findAllByType(String type);
}
