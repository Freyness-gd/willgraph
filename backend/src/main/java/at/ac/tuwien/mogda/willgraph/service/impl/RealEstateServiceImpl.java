package at.ac.tuwien.mogda.willgraph.service.impl;

import at.ac.tuwien.mogda.willgraph.controller.dto.AddressDto;
import at.ac.tuwien.mogda.willgraph.controller.dto.ListingCriteria;
import at.ac.tuwien.mogda.willgraph.controller.dto.ListingSearchFilterDto;
import at.ac.tuwien.mogda.willgraph.controller.dto.ListingWithScore;
import at.ac.tuwien.mogda.willgraph.controller.dto.PriorityItemDto;
import at.ac.tuwien.mogda.willgraph.controller.dto.RealEstateDto;
import at.ac.tuwien.mogda.willgraph.controller.dto.RealEstateWithScoreDto;
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
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

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
    public List<RealEstateWithScoreDto> searchWithFilters(ListingSearchFilterDto filter) throws NotFoundException {
        List<Map<String, Object>> weightedAmenities = getWeightedAmenities(filter.getAmenityPriorities());
        List<Map<String, Object>> weightedPois = getWeightedPois(filter.getPoiPriorities());
        ListingCriteria listingCriteria = filter.getListing();
        RegionEntity region = this.regionRepository.findByName(listingCriteria.getRegion()).orElseThrow(
            () -> new NotFoundException("Region " + listingCriteria.getRegion() + " not found")
        );
        Geometry regionPolygon = region.getGeometry();
        Envelope envelope = regionPolygon.getEnvelopeInternal();
        List<ListingWithScore> candidates = listingRepository.searchListings(
            envelope.getMinX(), envelope.getMinY(), envelope.getMaxX(), envelope.getMaxY(),
            listingCriteria.getMinPrice() != null ? listingCriteria.getMinPrice() : 0.0,
            listingCriteria.getMaxPrice() != null ? listingCriteria.getMaxPrice() : Double.MAX_VALUE,
            listingCriteria.getMinArea() != null ? listingCriteria.getMinArea() : 0.0,
            filter.getTransport() != null ? filter.getTransport().getMaxDistanceToStation() : 1000.0,
            weightedAmenities,
            weightedPois
        );

        List<RealEstateWithScoreDto> realEstateWithScoreDtos = candidates.stream()
            .peek(candidate -> {
                ListingEntity l = candidate.getListing();
                if (candidate.getAddress() != null) {
                    log.info("Address={}", candidate.getAddress());
                    l.setAddress(candidate.getAddress());
                }
            })
            .filter(candidate -> {
                ListingEntity listing = candidate.getListing();
                var neoPoint = listing.getAddress().getLocation();
                var jtsPoint = geometryFactory.createPoint(new Coordinate(neoPoint.getLongitude(), neoPoint.getLatitude()));
                return regionPolygon.contains(jtsPoint);
            })
            .limit(50)
            .map(result -> new RealEstateWithScoreDto(toDto(result.getListing()), result.getScore()))
            .toList();
        log.info("Score={}", realEstateWithScoreDtos.getFirst().getScore());
        double minScore = realEstateWithScoreDtos.stream().mapToDouble(RealEstateWithScoreDto::getScore).min().orElse(0.0);
        double maxScore = realEstateWithScoreDtos.stream().mapToDouble(RealEstateWithScoreDto::getScore).max().orElse(0.0);

        // If all scores are equal (or list empty), assign 1.0 to every item to indicate equal relative score.
        if (!realEstateWithScoreDtos.isEmpty()) {
            double range = maxScore - minScore;
            for (RealEstateWithScoreDto dto : realEstateWithScoreDtos) {
                Double rawScore = dto.getScore();
                double value = rawScore != null ? rawScore : 0.0;
                double normalized;
                if (range <= 0.0) {
                    normalized = 1.0;
                } else {
                    normalized = (value - minScore) / range;
                }
                dto.setScore(normalized);
            }
        }

        return realEstateWithScoreDtos;
    }

    @Override
    public RealEstateDto findById(String id) throws NotFoundException {
        ListingEntity listing = this.listingRepository.findById(id).orElseThrow(() -> new NotFoundException("Not found"));
        return toDto(listing);
    }

    private List<Map<String, Object>> getWeightedAmenities(List<PriorityItemDto> amenitiesPriority) {
        List<Map<String, Object>> weightedAmenities = new ArrayList<>();
        if (amenitiesPriority == null || amenitiesPriority.isEmpty()) {
            return Collections.emptyList();
        }
        for (int i = 0; i < amenitiesPriority.size(); i++) {
            PriorityItemDto item = amenitiesPriority.get(i);
            if (item.getCategoryValue() != null) {
                weightedAmenities.add(Map.of(
                    "name", item.getCategoryValue(),
                    "weight", item.getBonusScoreFactor() != null ? item.getBonusScoreFactor() : 1.0 //TODO: Could also use calculateWeight here
                ));
            }
        }
        return weightedAmenities;
    }

    private List<Map<String, Object>> getWeightedPois(List<PriorityItemDto> poisPriority) {
        List<Map<String, Object>> weightedPois = new ArrayList<>();
        if (poisPriority == null || poisPriority.isEmpty()) {
            return Collections.emptyList();
        }
        for (int i = 0; i < poisPriority.size(); i++) {
            PriorityItemDto item = poisPriority.get(i);
            if (item.getLat() != null && item.getLng() != null) {
                weightedPois.add(Map.of(
                    "lat", item.getLat(),
                    "lng", item.getLng(),
                    "weight", item.getBonusScoreFactor() != null ? item.getBonusScoreFactor() : 1.0
                ));
            }
        }
        return weightedPois;
    }

    //TODO: Check calculation if ok for guys and usable
    private Double calculateWeight(int index) {
        return Math.max(0.5, 2.0 - (index * 0.5));
    }

    private RealEstateDto toDto(ListingEntity listing) {
        AddressEntity address = listing.getAddress();
        Double totalArea = listing.getTotalArea() != null ? listing.getTotalArea() : listing.getLivingArea();
        RealEstateDto dto = RealEstateDto.builder()
            .id(listing.getId())
            .price(listing.getPrice())
            .title(listing.getTitle())
            .pricePerM2(listing.getPricePerM2() != null ? listing.getPricePerM2() : listing.getPrice() / totalArea)
            .bathroomCount(listing.getBathroomCount())
            .livingArea(listing.getLivingArea())
            .totalArea(totalArea)
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

    private AddressDto toDto(AddressEntity entity) {
        Double minDistance = null;
        if (entity.getNearbyStations() != null && !entity.getNearbyStations().isEmpty()) {
            minDistance = entity.getNearbyStations().stream()
                .mapToDouble(at.ac.tuwien.mogda.willgraph.entity.TransportConnection::getDistanceInMeters)
                .min()
                .orElse(Double.MAX_VALUE);
        }
        return AddressDto.builder()
            .osmId(entity.getOsmId())
            .city(entity.getCity())
            .countryCode(entity.getCountryCode())
            .houseNumber(entity.getHouseNumber())
            .street(entity.getStreet())
            .fullAddressString(entity.getFullAddressString())
            .location(entity.getLocation())
            .distanceToNearestStation(minDistance)
            .build();
    }
}
