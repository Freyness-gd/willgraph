package at.ac.tuwien.mogda.willgraph.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration for the application's amenity search bounding box
 */
@Component
@ConfigurationProperties(prefix = "app.amenity")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AmenitySearchConfig {

  private BoundingBox boundingBox = new BoundingBox();
  private Boolean autoFetchOnStartup = true;

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  public static class BoundingBox {
    private Double minLat;
    private Double minLon;
    private Double maxLat;
    private Double maxLon;

    @Override
    public String toString() {
      return "BoundingBox{" +
          "minLat=" + minLat +
          ", minLon=" + minLon +
          ", maxLat=" + maxLat +
          ", maxLon=" + maxLon +
          '}';
    }
  }

}

