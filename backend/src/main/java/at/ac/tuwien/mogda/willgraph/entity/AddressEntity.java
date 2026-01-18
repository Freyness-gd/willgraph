package at.ac.tuwien.mogda.willgraph.entity;

import lombok.*;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;
import org.springframework.data.neo4j.core.support.UUIDStringGenerator;
import org.springframework.data.neo4j.types.GeographicPoint2d;

import java.util.Set;

import static org.springframework.data.neo4j.core.schema.Relationship.Direction.OUTGOING;

@Node("Address")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddressEntity {
    @Id
    @GeneratedValue(generatorClass = UUIDStringGenerator.class)
    private String id;

    private String fullAddressString;

    private String street;
    private String houseNumber;
    private String postalCode;
    private String city;
    private String countryCode;

    private Long osmId;

    private GeographicPoint2d location;

    @Relationship(type = "IN_REGION", direction = OUTGOING)
    private RegionEntity region;

    @Relationship(type = "CLOSE_TO_STATION", direction = OUTGOING)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Set<TransportConnection> nearbyStations;

    @Relationship(type = "CLOSE_TO_POI", direction = OUTGOING)
    private Set<AmenityConnection> nearbyAmenities;
}
