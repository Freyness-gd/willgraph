package at.ac.tuwien.mogda.willgraph.entity;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.geo.Point;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

import java.util.Set;

import static org.springframework.data.neo4j.core.schema.Relationship.Direction.OUTGOING;


@Node("Address")
@Data
@Builder
public class AddressEntity {
    @Id
    @GeneratedValue
    private Long id;

    private String fullAddressString;

    private String street;
    private String houseNumber;
    private String postalCode;
    private String city;
    private String countryCode;

    private Long osmId;

    private Point location;

    //TODO: IF we want to include the region / Real estate here?
    @Relationship(type = "IN_REGION", direction = OUTGOING)
    private RegionEntity region;

    @Relationship(type = "CLOSE_TO_STATION", direction = OUTGOING)
    private Set<TransportConnection> nearbyStations;

    public void setLocation(Double latitude, Double longitude) {
        if (latitude != null && longitude != null) {
            this.location = new Point(longitude, latitude);
        }
    }
}
