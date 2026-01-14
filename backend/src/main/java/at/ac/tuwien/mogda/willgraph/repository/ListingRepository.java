package at.ac.tuwien.mogda.willgraph.repository;

import at.ac.tuwien.mogda.willgraph.entity.ListingEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
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

    @Query("MATCH (l:Listing {id: $listingId})-[:LOCATED_AT]->(a:Address) RETURN a.id")
    Optional<String> findAddressIdByListingId(@Param("listingId") String listingId);


    //MATCH (l:Listing {id: $listingId})-[:LOCATED_AT]->(a:Address)
    //
    //// 1. Calculate Amenities (Direct Walk)
    //OPTIONAL MATCH (a)-[r_poi:CLOSE_TO_POI]->(poi:PointOfInterest)-[:IS_TYPE]->(type:Amenity)
    //WHERE type.name = 'Supermarket'
    //WITH l, a, min(r_poi.distanceInMeters) AS distToSupermarket
    //
    //// 2. Calculate Public Transport Path (Walk + Ride)
    //// Find the closest station to the house
    //MATCH (a)-[r_walk:CLOSE_TO_STATION]->(startNode:Transport)
    //// Find the target station (e.g., City Center)
    //MATCH (targetNode:Transport {name: 'Stephansplatz'})
    //
    //// Find the shortest path through the transport network
    //MATCH path = shortestPath((startNode)-[:CONNECTED_TO*..10]->(targetNode))
    //
    //// Sum up the hops (assuming 2 mins per station hop for simplicity if no time data exists)
    //WITH l, distToSupermarket, r_walk.walkingDurationInMinutes AS walkToStation, path, length(path) * 2.0 AS rideTime
    //
    //// 3. Return the Combined Result
    //RETURN
    //    l.title,
    //    distToSupermarket AS metersToSupermarket,
    //    (walkToStation + rideTime) AS totalCommuteToCenterMin,
    //    [n in nodes(path) | n.name] AS stationsOnPath
    //ORDER BY totalCommuteToCenterMin ASC
    //LIMIT 1

}
