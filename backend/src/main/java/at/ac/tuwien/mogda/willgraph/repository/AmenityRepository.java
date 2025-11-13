package at.ac.tuwien.mogda.willgraph.repository;

import at.ac.tuwien.mogda.willgraph.entity.AmenityEntity;
import org.springframework.data.neo4j.repository.Neo4jRepository;

public interface AmenityRepository extends Neo4jRepository<AmenityEntity, String> {

}
