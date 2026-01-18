package at.ac.tuwien.mogda.willgraph.repository;

import at.ac.tuwien.mogda.willgraph.controller.dto.StationDistanceDto;
import at.ac.tuwien.mogda.willgraph.entity.TransportEntity;
import org.neo4j.driver.types.Path;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransportRepository extends Neo4jRepository<TransportEntity, String> {
    @Query("MATCH (t:Transport) " +
        "WHERE point.distance(t.location, point({latitude: $lat, longitude: $lon})) < $radiusMeters " +
        "WITH t, point.distance(t.location, point({latitude: $lat, longitude: $lon})) AS dist " +
        "RETURN t.name AS name, t.type AS type, t.line AS line, " +
        "       dist AS distanceInMeters, " +
        "       (dist / 80.0) AS walkingDurationInMinutes, " +
        "       t.location as location " +
        "ORDER BY dist ASC")
    List<StationDistanceDto> findStationsByLocation(double lat, double lon, double radiusMeters);

    @Query("MATCH p=shortestPath((start:Transport {id: $startId})-[*]-(end:Transport {id: $endId})) RETURN p")
    List<Path> findRoute(@Param("startId") String startId, @Param("endId") String endId);

    @Query("CREATE POINT INDEX transport_loc_idx IF NOT EXISTS FOR (t:Transport) ON (t.location)")
    void createSpatialIndex();

    @Query("MATCH (s1:Transport) " +
        "WHERE s1.location IS NOT NULL " +
        "WITH s1 " +
        "MATCH (s2:Transport) " +
        "WHERE point.distance(s1.location, s2.location) <= 500 " +
        "AND id(s1) < id(s2) " +
        "AND NOT (s1)-[:CONNECTED_TO]-(s2) " +
        "MERGE (s1)-[r:WALK]-(s2) " +
        "SET r.distance = point.distance(s1.location, s2.location)")
    void generateWalkEdges();

    @Query("MATCH (s1:Transport) " +
        "WHERE id(s1) IN $batchIds " +
        "AND s1.location IS NOT NULL " +
        "WITH s1 " +
        "MATCH (s2:Transport) " +
        "WHERE point.distance(s1.location, s2.location) <= 500 " +
        "AND id(s1) < id(s2) " +
        "AND NOT (s1)-[:CONNECTED_TO]-(s2) " +
        "MERGE (s1)-[r:WALK]-(s2) " +
        "SET r.distance = point.distance(s1.location, s2.location)")
    void generateWalkEdgesForBatch(@Param("batchIds") List<Long> batchIds);


    @Query("MATCH (t:Transport) RETURN id(t)")
    List<Long> getAllTransportIds();
}
