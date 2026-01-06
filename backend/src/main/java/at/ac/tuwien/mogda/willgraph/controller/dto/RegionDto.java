package at.ac.tuwien.mogda.willgraph.controller.dto;

import lombok.Data;

@Data
public class RegionDto {
    //ISO + Name probably unique...
    private String name;
    private String iso;
}
