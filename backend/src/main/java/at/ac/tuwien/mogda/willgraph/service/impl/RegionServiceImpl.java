package at.ac.tuwien.mogda.willgraph.service.impl;

import at.ac.tuwien.mogda.willgraph.controller.dto.RegionDto;
import at.ac.tuwien.mogda.willgraph.entity.RegionEntity;
import at.ac.tuwien.mogda.willgraph.exception.NotFoundException;
import at.ac.tuwien.mogda.willgraph.repository.RegionRepository;
import at.ac.tuwien.mogda.willgraph.service.RegionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class RegionServiceImpl implements RegionService {

    private final RegionRepository regionRepository;

    @Override
    public List<RegionDto> getAllRegions(Integer limit) {
        return regionRepository.findAll(PageRequest.of(0, limit))
                .getContent()
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    public RegionDto getRegionById(Long id) throws NotFoundException {
        return toDto(regionRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Region with ID: " + id + " not found!")));
    }

    @Override
    public List<RegionDto> searchRegions(String query, Integer limit) {
        if (query == null || query.isBlank()) {
            return getAllRegions(limit);
        }

        return regionRepository.searchByNameOrIso(query.trim(), limit)
                .stream()
                .map(this::toDto)
                .toList();
    }


    private RegionDto toDto(RegionEntity region) {
        return RegionDto.builder()
                .id(region.getId())
                .name(region.getName())
                .iso(region.getIso())
                .geometry(region.getGeometry())
                .center(region.getCenter()).build();
    }


}
