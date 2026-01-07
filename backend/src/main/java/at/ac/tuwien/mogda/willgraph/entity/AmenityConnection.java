package at.ac.tuwien.mogda.willgraph.entity;

import lombok.Data;
import org.springframework.data.neo4j.core.schema.RelationshipId;
import org.springframework.data.neo4j.core.schema.RelationshipProperties;
import org.springframework.data.neo4j.core.schema.TargetNode;

@RelationshipProperties
@Data
public class AmenityConnection {
    @RelationshipId
    private Long id;

    private Double distanceInMeters;
    private Double walkingDurationMinutes;
    @TargetNode
    private PointOfInterestEntity pointOfInterest;
}
