package at.ac.tuwien.mogda.willgraph.controller.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RealEstateDto {
    private String id;
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
    private AddressDto address;

}
