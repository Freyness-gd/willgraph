package at.ac.tuwien.mogda.willgraph.entity;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.geo.Point;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;
import org.springframework.data.neo4j.core.support.UUIDStringGenerator;

import static org.springframework.data.neo4j.core.schema.Relationship.Direction.OUTGOING;

@Node("Transport")
@Data
@Builder
public class TransportEntity {
    @Id
    @GeneratedValue(generatorClass = UUIDStringGenerator.class)
    private String id;
    private String name; //
    private String type; //Such as "Bus", "Tram", ...
    private Point location;
    @Relationship(type = "HAS_ADDRESS", direction = OUTGOING)
    private AddressEntity address;

}
