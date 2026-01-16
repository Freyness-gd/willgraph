package at.ac.tuwien.mogda.willgraph.repository;

import at.ac.tuwien.mogda.willgraph.entity.AmenityTypeEntity;
import java.util.Optional;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AmenityTypeRepository extends Neo4jRepository<AmenityTypeEntity, String> {


  Optional<AmenityTypeEntity> findByName(String name);
}
