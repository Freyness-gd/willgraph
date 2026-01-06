package at.ac.tuwien.mogda.willgraph.entity;

import lombok.Builder;
import lombok.Data;
import org.locationtech.jts.geom.Geometry;
import org.springframework.data.geo.Point;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;

@Node("Region")
@Data
@Builder
public class RegionEntity {
    @Id
    @GeneratedValue
    private Long id;
    private String name; //TODO: Do we want to set it on prefix based on postal code?
    private String iso;
    private Point center;
    //TODO: Add shape for region? --> See open street map shape?
    private Geometry geometry;

    //TODO: Might be calculated stat to be saved later on?
    private Double averagePricePerSqm;

    //TODO: Maybe remove?
//    @Relationship(type = "AVAILABLE_REAL_ESTATE", direction = OUTGOING)
//    private Set<AvailableRealEstate> addresses;
}
