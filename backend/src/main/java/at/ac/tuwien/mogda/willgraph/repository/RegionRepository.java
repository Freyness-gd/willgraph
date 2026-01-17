package at.ac.tuwien.mogda.willgraph.repository;

import at.ac.tuwien.mogda.willgraph.entity.RegionEntity;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RegionRepository extends Neo4jRepository<RegionEntity, String> {

    Optional<RegionEntity> findByName(String regionName);

    @Query("MATCH (r:Region) " +
            "WHERE toLower(r.name) CONTAINS toLower($query) " +
            "OR toLower(r.iso) CONTAINS toLower($query) " +
            "RETURN r " +
            "LIMIT $limit")
    List<RegionEntity> searchByNameOrIso(@Param("query") String query, @Param("limit") Integer limit);
}
