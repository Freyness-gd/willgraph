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
            "       (dist / 80.0) AS walkingDurationInMinutes " +
            "ORDER BY dist ASC")
    List<StationDistanceDto> findStationsByLocation(double lat, double lon, double radiusMeters);

    @Query("MATCH p=shortestPath((start:Transport {id: $startId})-[*]-(end:Transport {id: $endId})) RETURN p")
    List<Path> findRoute(@Param("startId") String startId, @Param("endId") String endId);
}
