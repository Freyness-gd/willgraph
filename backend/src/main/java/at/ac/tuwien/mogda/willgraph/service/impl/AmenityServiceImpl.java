package at.ac.tuwien.mogda.willgraph.service.impl;

import at.ac.tuwien.mogda.willgraph.controller.dto.AmenityOverviewDto;
import at.ac.tuwien.mogda.willgraph.controller.dto.OverpassResponse;
import at.ac.tuwien.mogda.willgraph.entity.AmenityTypeEntity;
import at.ac.tuwien.mogda.willgraph.entity.PointOfInterestEntity;
import at.ac.tuwien.mogda.willgraph.repository.AmenityTypeRepository;
import at.ac.tuwien.mogda.willgraph.repository.PoIRepository;
import at.ac.tuwien.mogda.willgraph.service.AmenityService;
import at.ac.tuwien.mogda.willgraph.service.OverpassApiService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Service
@Slf4j
@RequiredArgsConstructor
public class AmenityServiceImpl implements AmenityService {

    private final OverpassApiService overpassApiService;
    private final AmenityTypeRepository amenityTypeRepository;
    private final PoIRepository poiRepo;
    // Cache for pre-fetched amenities
    private final OverpassAmenityMapper mapper;

    @Override
    public Set<String> getSupportedAmenityTypes() {
        return overpassApiService.getSupportedAmenityTypes();
    }


    @Transactional
    public List<PointOfInterestEntity> importAmenityNodes(Double minLat, Double minLon, Double maxLat, Double maxLon) {
        OverpassResponse response = overpassApiService.query(minLat, minLon, maxLat, maxLon).block();
        if (response == null || response.elements() == null) {
            return List.of();
        }

        List<PointOfInterestEntity> pois = response.elements().stream()
            .filter(mapper::isNodeAmenity)
            .map(e -> {
                String amenity = mapper.amenityValue(e);

                AmenityTypeEntity type = amenityTypeRepository.findByName(amenity).orElseGet(() -> amenityTypeRepository.save(mapper.toAmenityType(amenity)));
                var address = mapper.toAddress(e);
                return mapper.toPoi(e, type, address);
            })
            .toList();

        return poiRepo.saveAll(pois);
    }

    @Override
    public List<AmenityOverviewDto> findAmenityOverview(double lat, double lng, double radius) {
        return this.amenityTypeRepository.findAmenityOverview(lat, lng, radius);
    }


}

