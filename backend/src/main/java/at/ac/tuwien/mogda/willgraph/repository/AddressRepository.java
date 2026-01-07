package at.ac.tuwien.mogda.willgraph.repository;

import at.ac.tuwien.mogda.willgraph.controller.dto.StationDistanceDto;
import at.ac.tuwien.mogda.willgraph.entity.AddressEntity;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AddressRepository extends Neo4jRepository<AddressEntity, String> {
    Optional<AddressEntity> findByOsmId(Long osmId);

    @Query("MATCH (a:Address {id: $addressId}) " +
            "MATCH (t:Transport) " +
            "WHERE point.distance(a.location, t.location) < 800 " +
            "MERGE (a)-[r:CLOSE_TO_STATION]->(t) " +
            "SET r.distanceInMeters = point.distance(a.location, t.location), " +
            "    r.walkingDurationInMinutes = point.distance(a.location, t.location) / 80.0")
    void generatePoximityLinks(@Param("addressId") String addressId);

    @Query("MATCH (a:Address) WHERE NOT (a)-[:CLOSE_TO_STATION]->() " +
            "MATCH (t:Transport) " +
            "WHERE point.distance(a.location, t.location) < 800 " +
            "MERGE (a)-[r:CLOSE_TO_STATION]->(t) " +
            "SET r.distanceInMeters = point.distance(a.location, t.location), " +
            "    r.walkingDurationInMinutes = point.distance(a.location, t.location) / 80.0")
    void generateAllProximityLinks();

    @Query("MATCH (a:Address {id: $addressId})-[r:CLOSE_TO_STATION]->(t:Transport) " +
            "RETURN t.name AS name, " +
            "       t.type AS type, " +
            "       t.line AS line, " +
            "       r.distanceInMeters AS distanceInMeters, " +
            "       r.walkingDurationInMinutes AS walkingDurationInMinutes " +
            "ORDER BY r.distanceInMeters ASC")
    List<StationDistanceDto> findStationsNearAddress(@Param("addressId") String addressId);

    @Query("MATCH (t:Transport) " +
            "WHERE point.distance(t.location, point({latitude: $lat, longitude: $lon})) < $radiusMeters " +
            "WITH t, point.distance(t.location, point({latitude: $lat, longitude: $lon})) AS dist " +
            "RETURN t.name AS name, t.type AS type, t.line AS line, " +
            "       dist AS distanceInMeters, " +
            "       (dist / 80.0) AS walkingDurationInMinutes " +
            "ORDER BY dist ASC LIMIT 10")
    List<StationDistanceDto> findStationsByLocation(double lat, double lon, double radiusMeters);
}
