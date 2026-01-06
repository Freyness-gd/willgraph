package at.ac.tuwien.mogda.willgraph.service;

import at.ac.tuwien.mogda.willgraph.controller.dto.RealEstateDto;

import java.util.List;

public interface RealEstateService {
    RealEstateDto findById(Long id);

    List<RealEstateDto> findRealEstatesInRegion(String regionName, String iso);

    List<RealEstateDto> findAll();
}
