package at.ac.tuwien.mogda.willgraph.controller.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.neo4j.types.GeographicPoint2d;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class StationDistanceDto {
    private String name;
    private String type;
    private String line;
    private Double distanceInMeters;
    private Double walkingDurationInMinutes;
    private Double travelTimeInMinutes;
    private GeographicPoint2d location;
    private String segmentType;
}
