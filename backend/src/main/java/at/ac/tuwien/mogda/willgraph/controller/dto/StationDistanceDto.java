package at.ac.tuwien.mogda.willgraph.controller.dto;

import lombok.Data;
import org.springframework.data.neo4j.types.GeographicPoint2d;

@Data
public class StationDistanceDto {
    private String name;
    private String type;
    private String line;
    private Double distanceInMeters;
    private Double walkingDurationInMinutes;
    private GeographicPoint2d location;
}
