package at.ac.tuwien.mogda.willgraph.controller.dto;

import java.util.Map;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record Element(
    String type,
    long id,
    Double lat,
    Double lon,
    Map<String, String> tags
) {


}