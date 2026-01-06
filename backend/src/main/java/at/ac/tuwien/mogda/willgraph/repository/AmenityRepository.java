package at.ac.tuwien.mogda.willgraph.repository;

import at.ac.tuwien.mogda.willgraph.entity.AmenityTypeEntity;
import org.springframework.data.neo4j.repository.Neo4jRepository;

public interface AmenityRepository extends Neo4jRepository<AmenityTypeEntity, String> {

}
