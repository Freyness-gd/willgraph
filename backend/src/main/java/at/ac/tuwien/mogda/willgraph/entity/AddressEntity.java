package at.ac.tuwien.mogda.willgraph.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.support.UUIDStringGenerator;

@Node("Address")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AddressEntity {
    @Id
    @GeneratedValue(generatorClass = UUIDStringGenerator.class)
    private String id;
    private Double latitude;
    private Double longitude;
    private String city;
    private String countryCode;
    private String postalCode;
    private String street;
    private String houseNumber;

    public AddressEntity(Double latitude, Double longitude, String city, String countryCode, String postalCode, String street, String houseNumber) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.city = city;
        this.countryCode = countryCode;
        this.postalCode = postalCode;
        this.street = street;
        this.houseNumber = houseNumber;
    }
}
