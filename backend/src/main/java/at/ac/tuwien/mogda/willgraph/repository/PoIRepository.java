package at.ac.tuwien.mogda.willgraph.repository;

import at.ac.tuwien.mogda.willgraph.entity.AmenityTypeEntity;
import at.ac.tuwien.mogda.willgraph.entity.PointOfInterestEntity;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PoIRepository extends Neo4jRepository<PointOfInterestEntity, String> {
    List<PointOfInterestEntity> findAllByType(AmenityTypeEntity type);
}
