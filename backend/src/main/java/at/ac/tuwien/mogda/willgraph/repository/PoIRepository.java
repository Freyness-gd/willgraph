package at.ac.tuwien.mogda.willgraph.repository;

import at.ac.tuwien.mogda.willgraph.controller.dto.PoIDistanceDto;
import at.ac.tuwien.mogda.willgraph.controller.dto.TransportPathDto;
import at.ac.tuwien.mogda.willgraph.entity.AmenityTypeEntity;
import at.ac.tuwien.mogda.willgraph.entity.PointOfInterestEntity;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

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

    @Query("WITH point({latitude: $fromLat, longitude: $fromLon}) AS startPoint, " +
        "point({latitude: $toLat, longitude: $toLon}) AS endPoint " +
        "CALL (startPoint) { " +
        "WITH startPoint " +
        "MATCH (s1:Transport) " +
        "WHERE point.distance(s1.location, startPoint) < $maxWalkDistance " +
        "RETURN s1, point.distance(s1.location, startPoint) AS walkToStation " +
        "ORDER BY walkToStation ASC " +
        "LIMIT 50 " +
        "} " +
        "CALL (endPoint) { " +
        "WITH endPoint " +
        "MATCH (s2:Transport) " +
        "WHERE point.distance(s2.location, endPoint) < $maxWalkDistance " +
        "RETURN s2, point.distance(s2.location, endPoint) AS walkFromStation " +
        "ORDER BY walkFromStation ASC " +
        "LIMIT 50 " +
        "} " +
        "WITH s1, s2, walkToStation, walkFromStation " +
        "WHERE s1 <> s2 " +
        "MATCH p = shortestPath((s1)-[:CONNECTED_TO|WALK*..50]-(s2)) " +
        "WITH p, s1, s2, walkToStation, walkFromStation, length(p) AS hops " +
        "ORDER BY hops + (walkToStation + walkFromStation) / 1000.0 " +
        "LIMIT 1 " +
        "RETURN " +
        " hops as numberOfStops," +
        " walkToStation as walkToStationMeters, " +
        " walkFromStation as walkFromStationMeters, " +
        " [i IN range(0, length(p)) | { " +
        "      name: coalesce((nodes(p)[i]).name, 'Unknown Station'), " +
        "      location: (nodes(p)[i]).location, " +
        "      type: (nodes(p)[i]).type, " +
        "      line: coalesce((nodes(p)[i]).line, null), " +
        "      distanceInMeters: CASE " +
        "           WHEN i = 0 THEN 0.0 " +
        "           ELSE " +
        "             CASE type(relationships(p)[i-1]) " +
        "               WHEN 'WALK' THEN (relationships(p)[i-1]).distance " +
        "               ELSE (relationships(p)[i-1]).distanceInMeters " +
        "             END " +
        "      END, " +
        "      travelTimeInMinutes: CASE " +
        "           WHEN i = 0 THEN 0.0 " +
        "           ELSE " +
        "             CASE type(relationships(p)[i-1]) " +
        "               WHEN 'WALK' THEN (relationships(p)[i-1]).distance / 80.0 " +
        "               ELSE coalesce((relationships(p)[i-1]).travelTimeInMinutes, 0.0) " +
        "             END " +
        "      END," +
        "      segmentType: CASE " +
        "           WHEN i = 0 THEN 'START' " +
        "           ELSE toString(type(relationships(p)[i-1])) " +
        "      END " +
        " }] as stations ")
    Optional<TransportPathDto> findShortestTransportPath(
        @Param("fromLat") double fromLat,
        @Param("fromLon") double fromLon,
        @Param("toLat") double toLat,
        @Param("toLon") double toLon,
        @Param("maxWalkDistance") double maxWalkDistance
    );
}
