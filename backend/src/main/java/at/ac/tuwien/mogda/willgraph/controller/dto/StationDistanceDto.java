package at.ac.tuwien.mogda.willgraph.controller.dto;

import lombok.Data;

@Data
public class StationDistanceDto {
    private String name;
    private String type;
    private String line;
    private Double distanceInMeters;
    private Double walkingDurationInMinutes;
}
