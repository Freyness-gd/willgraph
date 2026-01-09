package at.ac.tuwien.mogda.willgraph.entity;

import lombok.*;
import org.springframework.data.annotation.Version;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.types.GeographicPoint2d;

@Node("Transport")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransportEntity {
    @Id
    private String id;
    @Version
    private Long version;
    private String name; //
    private String type; //Such as "Bus", "Tram", ...
    private String line;
    private GeographicPoint2d location;
// REMOVED Because of recursion, but still in graph (needed for path queries)
//    @Relationship(type = "CONNECTED_TO", direction = OUTGOING)
//    private Set<TransportLeg> nextStations;

}
