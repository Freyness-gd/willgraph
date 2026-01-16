package at.ac.tuwien.mogda.willgraph.repository;

import at.ac.tuwien.mogda.willgraph.controller.dto.AmenityOverviewDto;
import at.ac.tuwien.mogda.willgraph.entity.AmenityTypeEntity;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AmenityTypeRepository extends Neo4jRepository<AmenityTypeEntity, String> {


    Optional<AmenityTypeEntity> findByName(String name);

    @Query("MATCH (p:PointOfInterest) " +
        "WHERE point.distance(p.location, point({latitude: $lat, longitude: $lon})) < $radius " +
        "MATCH (p)-[:IS_TYPE]->(a:Amenity) " +
        "WITH a, p, point.distance(p.location, point({latitude: $lat, longitude: $lon})) AS dist " +
        "RETURN a.name AS name, " +
        "       count(p) AS count, " +
        "       min(dist) AS closestDistance, " +
        "       (min(dist) / 80.0) AS closestWalkingTime " +
        "ORDER BY closestDistance ASC")
    List<AmenityOverviewDto> findAmenityOverview(double lat, double lon, double radius);
}
