package at.ac.tuwien.mogda.willgraph.controller.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.neo4j.types.GeographicPoint2d;


@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PoIDistanceDto {
    private String id;
    private String name;
    private String amenityType;
    private Double distanceInMeters;
    private Double walkingDurationInMinutes;
    private GeographicPoint2d location;
}
