package at.ac.tuwien.mogda.willgraph.controller.dto;

import lombok.Data;

@Data
public class PriorityItemDto {
    private String categoryValue;
    private Double maxDistanceToAmenity;
    private Integer bonusScoreFactor;
    private Double lat;
    private Double lng;
}
