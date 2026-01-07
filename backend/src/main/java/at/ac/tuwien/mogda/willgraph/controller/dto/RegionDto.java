package at.ac.tuwien.mogda.willgraph.controller.dto;

import lombok.Builder;
import lombok.Data;
import org.locationtech.jts.geom.Geometry;
import org.springframework.data.geo.Point;

@Data
@Builder
public class RegionDto {
    //ISO + Name probably unique...
    private String name;
    private String iso;
    private Geometry geometry;
    private Point center;
}
