package at.ac.tuwien.mogda.willgraph.controller.dto;

import lombok.Builder;
import lombok.Data;
import org.locationtech.jts.geom.Geometry;
import org.springframework.data.neo4j.types.GeographicPoint2d;

@Data
@Builder
public class RegionDto {
    //ISO + Name probably unique...
    private String name;
    private String iso;
    private Geometry geometry;
    private GeographicPoint2d center;
}
