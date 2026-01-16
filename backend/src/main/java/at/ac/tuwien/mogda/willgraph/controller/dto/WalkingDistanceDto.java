package at.ac.tuwien.mogda.willgraph.controller.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WalkingDistanceDto {
    private String poiId;
    private String poiName;
    private Double targetLatitude;
    private Double targetLongitude;
    private Double distanceInMeters;
    private Double walkingDurationInMinutes;
}

