package at.ac.tuwien.mogda.willgraph.controller.dto;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.neo4j.types.GeographicPoint2d;

@Data
@Builder
public class AddressDto {
    private String fullAddressString;
    private String street;
    private String houseNumber;
    private String postalCode;
    private String city;
    private String countryCode;
    private Long osmId;
    private GeographicPoint2d location;
    private Double distanceToNearestStation;
}
