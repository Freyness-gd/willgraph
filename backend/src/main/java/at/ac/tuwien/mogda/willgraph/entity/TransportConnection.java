package at.ac.tuwien.mogda.willgraph.entity;

import lombok.*;
import org.springframework.data.neo4j.core.schema.RelationshipId;
import org.springframework.data.neo4j.core.schema.RelationshipProperties;
import org.springframework.data.neo4j.core.schema.TargetNode;

@RelationshipProperties
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class TransportConnection {
    @RelationshipId
    private Long id;
    private Double distanceInMeters;
    private Integer hops;
    private Double walkingDurationInMinutes;
    @TargetNode
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private TransportEntity transport;
}
