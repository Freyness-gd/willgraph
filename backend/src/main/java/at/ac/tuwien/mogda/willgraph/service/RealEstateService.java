package at.ac.tuwien.mogda.willgraph.service;

import at.ac.tuwien.mogda.willgraph.controller.dto.RealEstateDto;
import at.ac.tuwien.mogda.willgraph.exception.NotFoundException;

import java.util.List;

public interface RealEstateService {
    RealEstateDto findById(Long id);

    List<RealEstateDto> findRealEstatesInRegion(String region, String iso) throws NotFoundException;

    List<RealEstateDto> findAll();
}
