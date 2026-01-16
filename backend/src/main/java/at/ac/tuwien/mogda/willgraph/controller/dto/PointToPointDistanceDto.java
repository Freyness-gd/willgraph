wpackage at.ac.tuwien.mogda.willgraph.controller.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PointToPointDistanceDto {
    private Double fromLatitude;
    private Double fromLongitude;
    private Double toLatitude;
    private Double toLongitude;
    private Double distanceInMeters;
    private Double walkingDurationInMinutes;
}

