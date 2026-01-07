package at.ac.tuwien.mogda.willgraph.entity;

import lombok.Builder;
import lombok.Data;
import org.locationtech.jts.geom.Geometry;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.types.GeographicPoint2d;

@Node("Region")
@Data
@Builder
public class RegionEntity {
    @Id
    private String iso;
    private String name; //TODO: Do we want to set it on prefix based on postal code?
    private GeographicPoint2d center;
    private Geometry geometry;

    //TODO: Might be calculated stat to be saved later on?
    private Double averagePricePerSqm;
}
