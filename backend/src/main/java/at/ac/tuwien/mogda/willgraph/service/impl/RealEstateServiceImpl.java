package at.ac.tuwien.mogda.willgraph.service.impl;

import at.ac.tuwien.mogda.willgraph.controller.dto.AddressDto;
import at.ac.tuwien.mogda.willgraph.controller.dto.RealEstateDto;
import at.ac.tuwien.mogda.willgraph.controller.dto.StationDistanceDto;
import at.ac.tuwien.mogda.willgraph.entity.AddressEntity;
import at.ac.tuwien.mogda.willgraph.entity.ListingEntity;
import at.ac.tuwien.mogda.willgraph.entity.RegionEntity;
import at.ac.tuwien.mogda.willgraph.exception.NotFoundException;
import at.ac.tuwien.mogda.willgraph.repository.AddressRepository;
import at.ac.tuwien.mogda.willgraph.repository.ListingRepository;
import at.ac.tuwien.mogda.willgraph.repository.RegionRepository;
import at.ac.tuwien.mogda.willgraph.service.RealEstateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class RealEstateServiceImpl implements RealEstateService {

    private final ListingRepository listingRepository;
    private final AddressRepository addressRepository;
    private final RegionRepository regionRepository;
    private final GeometryFactory geometryFactory = new GeometryFactory();

    public List<RealEstateDto> findRealEstatesInRegion(String regionName, String iso) throws NotFoundException {
        RegionEntity region = this.regionRepository.findByName(regionName).orElseThrow(
                () -> new NotFoundException("Region " + regionName + " not found")
        );

        Geometry regionPolygon = region.getGeometry();
        var envelope = regionPolygon.getEnvelopeInternal();

        log.info("Searching Box: " +
                envelope.getMinX() + " to " + envelope.getMaxX() + " (Lon/X), " +
                envelope.getMinY() + " to " + envelope.getMaxY() + " (Lat/Y)"
        );
        List<ListingEntity> candidates = listingRepository.findInsideBoundingBox(
                envelope.getMinX(),
                envelope.getMinY(),
                envelope.getMaxX(),
                envelope.getMaxY()
        );
        return candidates.stream()
                .filter(listing -> {
                    var neoPoint = listing.getAddress().getLocation();
                    var jtsPoint = geometryFactory.createPoint(new Coordinate(neoPoint.getLongitude(), neoPoint.getLatitude()));
                    return regionPolygon.contains(jtsPoint);
                })
                .map(this::toDto)
                .toList();
    }

    @Override
    public List<RealEstateDto> findAll() {
        return this.listingRepository.findAll(PageRequest.of(0, 10)).getContent()
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    public List<StationDistanceDto> findStationsNearby(String id) throws NotFoundException {
        String addressId = this.listingRepository.findAddressIdByListingId(id).orElseThrow(
                () -> new NotFoundException("Listing with id: " + id + " not found!")
        );
        return this.addressRepository.findStationsNearAddress(addressId);
    }

    @Override
    public RealEstateDto findById(String id) throws NotFoundException {
        ListingEntity listing = this.listingRepository.findById(id).orElseThrow(() -> new NotFoundException("Not found"));
        return toDto(listing);
    }

    private RealEstateDto toDto(ListingEntity listing) {
        AddressEntity address = listing.getAddress();
        RealEstateDto dto = RealEstateDto.builder()
                .id(listing.getId())
                .price(listing.getPrice())
                .title(listing.getTitle())
                .pricePerM2(listing.getPricePerM2())
                .bathroomCount(listing.getBathroomCount())
                .livingArea(listing.getLivingArea())
                .totalArea(listing.getTotalArea())
                .roomCount(listing.getRoomCount())
                .externalUrl(listing.getExternalUrl())
                .timestampFound(listing.getTimestampFound())
                .source(listing.getSource())
                .build();
        if (address != null) {
            Double minDistance = null;
            if (address.getNearbyStations() != null && !address.getNearbyStations().isEmpty()) {
                minDistance = address.getNearbyStations().stream()
                        .mapToDouble(at.ac.tuwien.mogda.willgraph.entity.TransportConnection::getDistanceInMeters)
                        .min()
                        .orElse(Double.MAX_VALUE);
            }
            dto.setAddress(AddressDto.builder()
                    .osmId(address.getOsmId())
                    .city(address.getCity())
                    .countryCode(address.getCountryCode())
                    .houseNumber(address.getHouseNumber())
                    .street(address.getStreet())
                    .fullAddressString(address.getFullAddressString())
                    .location(address.getLocation())
                    .distanceToNearestStation(minDistance)
                    .build());
        }
        return dto;
    }
}
