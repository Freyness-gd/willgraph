package at.ac.tuwien.mogda.willgraph.repository;

import at.ac.tuwien.mogda.willgraph.entity.RegionEntity;
import org.springframework.data.neo4j.repository.Neo4jRepository;

import java.util.Optional;

public interface RegionRepository extends Neo4jRepository<RegionEntity, Long> {

    Optional<RegionEntity> findByName(String regionName);
}
