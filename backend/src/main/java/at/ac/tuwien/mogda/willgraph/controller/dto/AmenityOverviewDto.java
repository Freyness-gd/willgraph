package at.ac.tuwien.mogda.willgraph.controller.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class AmenityOverviewDto {
    private String name;
    private Long count;
    private Double closestDistance;
    private Double closestWalkingTime;
}
