package at.ac.tuwien.mogda.willgraph.controller.dto;

import lombok.Data;

@Data
public class ListingCriteria {
    private String region;
    private Double minPrice;
    private Double maxPrice;
    private Double minArea;
    private Double maxArea;
}
