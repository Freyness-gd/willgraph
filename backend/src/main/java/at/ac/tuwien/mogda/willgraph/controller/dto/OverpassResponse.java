package at.ac.tuwien.mogda.willgraph.controller.dto;

import java.util.List;
import org.springframework.data.geo.Point;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OverpassResponse(List<Element> elements) {

  public Point resolveLocation(Element e) {
    if (e.lat() == null || e.lon() == null) {
      throw new IllegalArgumentException("Expected node with lat/lon, got type=" + e.type() + " id=" + e.id());
    }
    return new Point(e.lon(), e.lat()); // (lon, lat)
  }
}