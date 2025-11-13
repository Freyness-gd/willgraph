package at.ac.tuwien.mogda.willgraph.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;
import org.springframework.data.neo4j.core.support.UUIDStringGenerator;

import java.util.HashSet;
import java.util.Set;

import static org.springframework.data.neo4j.core.schema.Relationship.Direction.OUTGOING;

@Node("PoI")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PointOfInterestEntity {
    @Id
    @GeneratedValue(generatorClass = UUIDStringGenerator.class)
    private String id;
    private int score;
    private String type;
    @Relationship(type = "HAS_ADDRESS", direction = OUTGOING)
    private AddressEntity address;
    @Relationship(type= "IS_A", direction = OUTGOING)
    private Set<AmenityEntity> amenities = new HashSet<>();

    public PointOfInterestEntity(int score, String type) {
        this.score = score;
        this.type = type;
        this.amenities = new HashSet<>();
    }
}
