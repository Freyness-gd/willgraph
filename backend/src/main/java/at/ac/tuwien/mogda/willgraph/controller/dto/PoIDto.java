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
public class PoIDto {
    private int score;
    private String type;
    private AddressCreateDto address;
}
