package at.ac.tuwien.mogda.willgraph.entity;

import lombok.Builder;
import lombok.Data;
import org.locationtech.jts.geom.Geometry;
import org.springframework.data.annotation.Version;
import org.springframework.data.geo.Point;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;

@Node("Region")
@Data
@Builder
public class RegionEntity {
    @Id
    private String iso;
    @Version
    private Long version;
    private String name; //TODO: Do we want to set it on prefix based on postal code?
    private Point center;
    //TODO: Add shape for region? --> See open street map shape?
    private Geometry geometry;

    //TODO: Might be calculated stat to be saved later on?
    private Double averagePricePerSqm;
}
