package at.ac.tuwien.mogda.willgraph.repository;

import at.ac.tuwien.mogda.willgraph.entity.ListingEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ListingRepository extends Neo4jRepository<ListingEntity, String> {
    @Query("MATCH (l:Listing)-[r:LOCATED_AT]->(a:Address) " +
            "WHERE a.location.x >= $minLon AND a.location.x <= $maxLon " +
            "AND   a.location.y >= $minLat AND a.location.y <= $maxLat " +
            "RETURN l, r, a")
    List<ListingEntity> findInsideBoundingBox(
            @Param("minLon") double minLon,
            @Param("minLat") double minLat,
            @Param("maxLon") double maxLon,
            @Param("maxLat") double maxLat);

    Page<ListingEntity> findAll(Pageable pageable);

}
