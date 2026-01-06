package at.ac.tuwien.mogda.willgraph.controller.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class RealEstateDto {
    private Long id;
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
    private LocalDateTime timestampFound;
    private AddressDto address;

}
