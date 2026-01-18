package at.ac.tuwien.mogda.willgraph.service.impl;// OverpassAmenityMapper.java

import at.ac.tuwien.mogda.willgraph.controller.dto.Element;
import at.ac.tuwien.mogda.willgraph.entity.AddressEntity;
import at.ac.tuwien.mogda.willgraph.entity.AmenityTypeEntity;
import at.ac.tuwien.mogda.willgraph.entity.PointOfInterestEntity;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.data.geo.Point;
import org.springframework.data.neo4j.types.GeographicPoint2d;
import org.springframework.stereotype.Component;

@Component
public class OverpassAmenityMapper {

  private static String firstNonBlank(String... values) {
    for (String v : values) {
      if (v != null && !v.isBlank()) return v;
    }
    return null;
  }

  public boolean isNodeAmenity(Element e) {
    return e != null
        && "node".equals(e.type())
        && e.tags() != null
        && e.tags().containsKey("amenity")
        && e.lat() != null
        && e.lon() != null;
  }

  public String amenityValue(Element e) {
    return Optional.ofNullable(e.tags()).map(t -> t.get("amenity")).orElse(null);
  }

  public AmenityTypeEntity toAmenityType(String amenity) {
    if (amenity == null || amenity.isBlank()) return null;
    return AmenityTypeEntity.builder()
        .name(amenity)
        //.score(0)
        .build();
  }

  public AddressEntity toAddress(Element e) {
    Map<String, String> t = Optional.ofNullable(e.tags()).orElse(Map.of());
    boolean hasAnyAddr =
        t.containsKey("addr:street")
            || t.containsKey("addr:housenumber")
            || t.containsKey("addr:city")
            || t.containsKey("addr:postcode");

    if (!hasAnyAddr) return null;

    return AddressEntity.builder()
        .street(t.get("addr:street"))
        .houseNumber(t.get("addr:housenumber"))
        .city(t.get("addr:city"))
        .postalCode(t.get("addr:postcode"))
        .build();
  }

  public PointOfInterestEntity toPoi(Element e, AmenityTypeEntity type, AddressEntity address) {
    Map<String, String> tags = Optional.ofNullable(e.tags()).orElse(Map.of());

    // Name fallbacks: name -> brand -> amenity value
    String amenity = tags.get("amenity");
    String name = firstNonBlank(tags.get("name"), tags.get("brand"), amenity, "Unnamed");

    // (lon, lat)
    GeographicPoint2d location = new GeographicPoint2d(e.lat(), e.lon());

    return PointOfInterestEntity.builder()
        .name(name)
        .score(null) // your scoring logic
        .location(location)
        .description(tags.get("description"))
        .osmId(e.id())
        .type(type)
        .address(address)
        .nearbyStations(List.of()) // fill later
        .build();
  }
}