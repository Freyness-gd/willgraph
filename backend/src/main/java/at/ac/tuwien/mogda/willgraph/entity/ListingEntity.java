package at.ac.tuwien.mogda.willgraph.entity;

import lombok.*;
import org.springframework.data.neo4j.core.schema.*;
import org.springframework.data.neo4j.core.support.UUIDStringGenerator;

import static org.springframework.data.neo4j.core.schema.Relationship.Direction.OUTGOING;

@Node("Listing")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ListingEntity {
    @Id
    @GeneratedValue(generatorClass = UUIDStringGenerator.class)
    private String id;

    @Property("url")
    private String externalUrl;
    private String title;

    private Double price;
    private Double pricePerM2;

    private Double livingArea;
    private Double totalArea;

    private Integer roomCount;
    private Integer bedroomCount;
    private Integer bathroomCount;

    private String source;
    private String timestampFound;
    //Removed timestamp is not really extractable

    @Relationship(type = "LOCATED_AT", direction = OUTGOING)
    private AddressEntity address;
    //TODO: Maybe add relationship to real estate type + rooms?
}
