package at.ac.tuwien.mogda.willgraph.entity;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;

@Node("Amenity")
@Data
@Builder
public class AmenityTypeEntity {
  @Id
  private String name;
}
