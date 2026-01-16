package at.ac.tuwien.mogda.willgraph.repository;

import at.ac.tuwien.mogda.willgraph.entity.ListingEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
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

    @Query("""
                    MATCH (l:Listing)-[:LOCATED_AT]->(a:Address)
                    WHERE a.location.x >= $minLon AND a.location.x <= $maxLon
                      AND a.location.y >= $minLat AND a.location.y <= $maxLat
                      AND l.price >= $minPrice\s
                      AND l.price <= $maxPrice\s
                      AND l.livingArea >= $minArea
        
                    // 1. Safe Start Node Retrieval
                    CALL (a) {
                         WITH a
                         MATCH (s:Transport)
                         WHERE point.distance(a.location, s.location) <= $maxDistTransport
                         RETURN s as startNode
                         ORDER BY point.distance(a.location, s.location) ASC
                         LIMIT 1
                         UNION
                         RETURN null AS startNode
                     }
        
                    // 2. AMENITY SCORE (Scoped Call)
                   CALL (a) {
                         WITH a, coalesce($amenities, []) as safeAmenities
                         WITH a, safeAmenities WHERE size(safeAmenities) = 0
                         RETURN 0.0 AS amenityScore
                         UNION
                         WITH a, coalesce($amenities, []) as safeAmenities
                         WITH a, safeAmenities WHERE size(safeAmenities) > 0
                         UNWIND safeAmenities AS item
                         // Use OPTIONAL MATCH to ensure row survives if no amenity found
                         OPTIONAL MATCH (poi:PointOfInterest)-[:IS_TYPE]->(t:Amenity)
                         WHERE t.name = item.name AND point.distance(a.location, poi.location) < 1000
                         WITH item, min(point.distance(a.location, poi.location)) AS minDist
                         RETURN sum(item.weight * (1000.0 - coalesce(minDist, 1000.0)) / 100.0) AS amenityScore
                     }
        
                    // 3. POI SCORE (Scoped Call)
                    // We explicitly pass 'l', 'a', 'startNode'
                   CALL (l, a, startNode) {
                        WITH l, a, startNode, coalesce($customPois, []) as safePois
                        WITH l, a, startNode, safePois WHERE size(safePois) = 0
                        RETURN 0.0 AS poiScore
                        UNION
                        WITH l, a, startNode, coalesce($customPois, []) as safePois
                        WITH l, a, startNode, safePois WHERE size(safePois) > 0
                        UNWIND safePois AS item
                        WITH startNode, item,
                             point.distance(a.location, point({latitude: item.lat, longitude: item.lng})) as distGeo
        
                        WITH startNode, item, distGeo,
                             CASE WHEN distGeo < 1500 THEN (1500.0 - distGeo) / 15.0 ELSE 0.0 END AS walkScore
        
                        // NESTED CALL FIX: Move logic inside CASE or keep stream alive
                        CALL (startNode, item, distGeo) {
                            WITH startNode, item, distGeo
                            // Only run pathfinding if conditions met
                            WHERE distGeo >= 1500 AND startNode IS NOT NULL
                            MATCH (s2:Transport)
                            WHERE point.distance(s2.location, point({latitude: item.lat, longitude: item.lng})) < 800
                            MATCH p = shortestPath((startNode)-[:CONNECTED_TO*..6]-(s2))
                            RETURN 50.0 - (length(p) * 5.0) AS rawTransScore
                            ORDER BY length(p) ASC
                            LIMIT 1
                            UNION
                            // This ensures we always return SOMETHING, even if WHERE above fails
                            RETURN 0.0 AS rawTransScore
                        }
                        WITH item, walkScore, max(rawTransScore) as transScore
                        RETURN sum(item.weight * (walkScore + transScore)) AS poiScore
            }
        
            WITH l, amenityScore, poiScore
            WITH l, (amenityScore + poiScore) AS totalScore
            ORDER BY totalScore DESC
            LIMIT 50
            OPTIONAL MATCH (l)-[r:LOCATED_AT]->(addr:Address)
            RETURN DISTINCT l, r, addr
        """)
    List<ListingEntity> searchListings(
        @Param("minLon") double minLon,
        @Param("minLat") double minLat,
        @Param("maxLon") double maxLon,
        @Param("maxLat") double maxLat,
        @Param("minPrice") Double minPrice,
        @Param("maxPrice") Double maxPrice,
        @Param("minArea") Double minArea,
        @Param("maxDistTransport") Double maxDistTransport,
        @Param("amenities") List<Map<String, Object>> amenities,
        @Param("customPois") List<Map<String, Object>> customPois
    );


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
