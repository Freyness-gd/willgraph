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

import static org.springframework.data.neo4j.core.schema.Relationship.Direction.INCOMING;

@Node("Amenity")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AmenityEntity {
    @Id
    @GeneratedValue(generatorClass = UUIDStringGenerator.class)
    private String id;
    private String name;
    private int score;
    private double maximumViableDistance;
    private String mobile;
    private String openingHours;
    private String website;
    @Relationship(type = "IS_A", direction = INCOMING)
    private PointOfInterestEntity poi;

    public AmenityEntity(double maximumViableDistance, int score, String name) {
        this.maximumViableDistance = maximumViableDistance;
        this.score = score;
        this.name = name;
    }
}
