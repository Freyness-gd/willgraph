package at.ac.tuwien.mogda.willgraph.entity;

import lombok.Data;
import org.springframework.data.neo4j.core.schema.RelationshipId;
import org.springframework.data.neo4j.core.schema.RelationshipProperties;
import org.springframework.data.neo4j.core.schema.TargetNode;

@RelationshipProperties
@Data
public class TransportConnection {
    @RelationshipId
    private Long id;
    private Double distanceInMeters;
    private Integer hops;
    private Double walkingDurationInMinutes;
    @TargetNode
    private TransportEntity transport;
}
