package at.ac.tuwien.mogda.willgraph.service.impl;

import at.ac.tuwien.mogda.willgraph.controller.dto.AddressCreateDto;
import at.ac.tuwien.mogda.willgraph.controller.dto.PoIDto;
import at.ac.tuwien.mogda.willgraph.entity.AddressEntity;
import at.ac.tuwien.mogda.willgraph.entity.PointOfInterestEntity;
import at.ac.tuwien.mogda.willgraph.repository.AddressRepository;
import at.ac.tuwien.mogda.willgraph.repository.PoIRepository;
import at.ac.tuwien.mogda.willgraph.service.PoIService;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;

@Service
public class PoIServiceImpl implements PoIService {

    private final PoIRepository poiRepository;
    private final AddressRepository addressRepository;

    public PoIServiceImpl(PoIRepository poiRepository,  AddressRepository addressRepository) {
        this.addressRepository = addressRepository;
        this.poiRepository = poiRepository;
    }

    @Override
    public List<PointOfInterestEntity> findAllOfType(String type) {
        return this.poiRepository.findAllByType(type);
    }

    @Override
    public PointOfInterestEntity create(PoIDto poi) {
        //TODO: Validation && Logger

        AddressCreateDto poiAddress = poi.getAddress();
        PointOfInterestEntity poiEntity = PointOfInterestEntity.builder()
            .score(poi.getScore())
            .type(poi.getType())
            .build();
        if (poiAddress != null) {
            AddressEntity address = AddressEntity.builder()
                .city(poiAddress.getCity())
                .countryCode(poiAddress.getCountryCode())
                .houseNumber(poiAddress.getHouseNumber())
                .street(poiAddress.getStreet())
                .latitude(poiAddress.getLatitude())
                .longitude(poiAddress.getLongitude())
                .postalCode(poiAddress.getPostalCode())
                .build();
            poiEntity.setAddress(address);
        }
        return this.poiRepository.save(poiEntity);
    }
}
