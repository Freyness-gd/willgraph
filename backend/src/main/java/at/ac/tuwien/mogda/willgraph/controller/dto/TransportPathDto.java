package at.ac.tuwien.mogda.willgraph.controller.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TransportPathDto {
    private int numberOfStops;
    private double walkToStationMeters;
    private double walkFromStationMeters;
    private List<StationDistanceDto> stations;
}
