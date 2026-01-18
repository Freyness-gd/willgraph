package at.ac.tuwien.mogda.willgraph.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.neo4j.core.schema.RelationshipId;
import org.springframework.data.neo4j.core.schema.RelationshipProperties;
import org.springframework.data.neo4j.core.schema.TargetNode;

@RelationshipProperties
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class TransportLeg {
    @RelationshipId
    private Long id;
    private Double travelTimeInMinutes;
    private Integer frequency;
    @TargetNode
    private TransportEntity nextStation;
}
