package at.ac.tuwien.mogda.willgraph.entity;

import lombok.Data;
import org.springframework.data.neo4j.core.schema.RelationshipId;
import org.springframework.data.neo4j.core.schema.RelationshipProperties;
import org.springframework.data.neo4j.core.schema.TargetNode;

@RelationshipProperties
@Data
public class AvailableRealEstate {
    @RelationshipId
    private Long id;
    private Double price;
    private Double area;
    //TODO: Missing fields?
    @TargetNode
    private AddressEntity address;
}
