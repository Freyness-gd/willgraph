package at.ac.tuwien.mogda.willgraph.controller.dto;

import lombok.Data;

import java.util.List;

@Data
public class ListingSearchFilterDto {
    private ListingCriteria listing;
    private TransportCriteria transport;
    private List<PriorityItemDto> amenityPriorities;
    private List<PriorityItemDto> poiPriorities;
    private Double bonusScoreRegion = 0.0;
}
