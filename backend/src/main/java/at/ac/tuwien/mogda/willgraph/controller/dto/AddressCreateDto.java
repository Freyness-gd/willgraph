package at.ac.tuwien.mogda.willgraph.controller.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AddressCreateDto {
    private Double latitude;
    private Double longitude;
    private String city;
    private String countryCode;
    private String postalCode;
    private String street;
    private String houseNumber;
}
