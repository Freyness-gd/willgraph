package at.ac.tuwien.mogda.willgraph.service;

import at.ac.tuwien.mogda.willgraph.controller.dto.RegionDto;
import at.ac.tuwien.mogda.willgraph.exception.NotFoundException;

import java.util.List;

public interface RegionService {
    List<RegionDto> getAllRegions(Integer limit);

    RegionDto getRegionById(Long id) throws NotFoundException;

    List<RegionDto> searchRegions(String query, Integer limit);
}
