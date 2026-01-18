package at.ac.tuwien.mogda.willgraph.controller.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AmenityCreateDto {
    private String name;
    private int score;
    private double maximumViableDistance;
    private String mobile;
    private String openingHours;
    private String website;
}
