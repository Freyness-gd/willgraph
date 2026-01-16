package at.ac.tuwien.mogda.willgraph.repository;

import at.ac.tuwien.mogda.willgraph.controller.dto.PoIDistanceDto;
import at.ac.tuwien.mogda.willgraph.entity.AmenityTypeEntity;
import at.ac.tuwien.mogda.willgraph.entity.PointOfInterestEntity;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PoIRepository extends Neo4jRepository<PointOfInterestEntity, String> {
    //TODO: IF you want we can add the score here?
    @Query("MATCH (p:PointOfInterest) " +
        "WHERE point.distance(p.location, point({latitude: $lat, longitude: $lon})) < $radiusMeters " +
        "WITH p, point.distance(p.location, point({latitude: $lat, longitude: $lon})) AS dist " +
        "OPTIONAL MATCH (p)-[:IS_TYPE]->(t) " +
        "RETURN p.id AS id, " +
        "       p.name AS name, " +
        "       coalesce(t.name, 'Unknown') AS amenityType, " +
        "       dist AS distanceInMeters, " +
        "       (dist / 80.0) AS walkingDurationInMinutes, " +
        "       p.location as location " +
        "ORDER BY dist ASC")
    List<PoIDistanceDto> findPoIsNearby(double lat, double lon, double radiusMeters);

    List<PointOfInterestEntity> findAllByType(AmenityTypeEntity type);
}
