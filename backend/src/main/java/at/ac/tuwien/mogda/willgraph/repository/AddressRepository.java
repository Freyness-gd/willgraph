package at.ac.tuwien.mogda.willgraph.repository;

import at.ac.tuwien.mogda.willgraph.entity.AddressEntity;
import org.springframework.data.neo4j.repository.Neo4jRepository;

import java.util.Optional;

public interface AddressRepository extends Neo4jRepository<AddressEntity, String> {
    Optional<AddressEntity> findByOsmId(Long osmId);
}
