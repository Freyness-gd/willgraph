package at.ac.tuwien.mogda.willgraph.controller.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.neo4j.types.GeographicPoint2d;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class StopsDto {
    private String name;
    private GeographicPoint2d location;
}
