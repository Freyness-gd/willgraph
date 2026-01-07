package at.ac.tuwien.mogda.willgraph.service.impl;

import at.ac.tuwien.mogda.willgraph.controller.dto.PoIDto;
import at.ac.tuwien.mogda.willgraph.entity.AmenityTypeEntity;
import at.ac.tuwien.mogda.willgraph.entity.PointOfInterestEntity;
import at.ac.tuwien.mogda.willgraph.repository.AddressRepository;
import at.ac.tuwien.mogda.willgraph.repository.AmenityTypeRepository;
import at.ac.tuwien.mogda.willgraph.repository.PoIRepository;
import at.ac.tuwien.mogda.willgraph.service.PoIService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PoIServiceImpl implements PoIService {

    private final PoIRepository poiRepository;
    private final AddressRepository addressRepository;
    private final AmenityTypeRepository amenityTypeRepository;

    @Override
    public List<PointOfInterestEntity> findAllOfType(String type) {
        AmenityTypeEntity typeEntity = this.amenityTypeRepository.findById(type).orElse(null);
        if (typeEntity == null) {
            return Collections.emptyList();
        }
        return this.poiRepository.findAllByType(typeEntity);
    }

    @Override
    public PointOfInterestEntity create(PoIDto poi) {
        //TODO: Validation && Logger

//        AddressCreateDto poiAddress = poi.getAddress();
//        PointOfInterestEntity poiEntity = PointOfInterestEntity.builder()
//            .score(poi.getScore())
//            .type(poi.getType())
//            .build();
//        if (poiAddress != null) {
//            AddressEntity address = AddressEntity.builder()
//                .city(poiAddress.getCity())
//                .countryCode(poiAddress.getCountryCode())
//                .houseNumber(poiAddress.getHouseNumber())
//                .street(poiAddress.getStreet())
//                .latitude(poiAddress.getLatitude())
//                .longitude(poiAddress.getLongitude())
//                .postalCode(poiAddress.getPostalCode())
//                .build();
//            poiEntity.setAddress(address);
//        }
//        return this.poiRepository.save(poiEntity);
        return null;
    }
}
