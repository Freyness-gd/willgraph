package at.ac.tuwien.mogda.willgraph.bootstrap;

import at.ac.tuwien.mogda.willgraph.entity.RegionEntity;
import at.ac.tuwien.mogda.willgraph.repository.RegionRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.geo.Point;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
@Order(1)
@Slf4j
public class RegionImporter implements CommandLineRunner {
    private final RegionRepository regionRepository;
    private final ObjectMapper objectMapper;
    private final GeometryFactory geometryFactory = new GeometryFactory();

    @Override
    public void run(String... args) throws Exception {
        if (regionRepository.count() > 0) {
            log.info("Regions already exist. Skipping import.");
            return;
        }

        log.info("Importing Regions from GeoJSON...");

        //TODO: Need to ensure it works in container
        try (InputStream inputStream = new ClassPathResource("bezirke_999_geo.json").getInputStream()) {
            JsonNode root = objectMapper.readTree(inputStream);
            JsonNode features = root.get("features");

            if (features.isArray()) {
                for (JsonNode feature : features) {
                    processFeature(feature);
                }
            }
        }
        //TODO: make it generally working for any region json -> aka directory
        try (InputStream inputStream = new ClassPathResource("gemeinden_999_geo.json").getInputStream()) {
            JsonNode root = objectMapper.readTree(inputStream);
            JsonNode features = root.get("features");

            if (features.isArray()) {
                for (JsonNode feature : features) {
                    processFeature(feature);
                }
            }
        }
        log.info("Region import completed.");
    }

    private void processFeature(JsonNode feature) {
        // 1. Extract Properties
        JsonNode properties = feature.get("properties");
        String name = properties.get("name").asText();
        String iso = properties.get("iso").asText();

        // 2. Extract Geometry (MultiPolygon)
        JsonNode geometryNode = feature.get("geometry");
        String type = geometryNode.get("type").asText();

        if (!"MultiPolygon".equalsIgnoreCase(type)) {
            log.warn("Skipping region {} - only MultiPolygon is supported, found {}", name, type);
            return;
        }

        // 3. Build JTS Geometry
        MultiPolygon multiPolygon = parseMultiPolygon(geometryNode.get("coordinates"));

        // 4. Calculate Center (Centroid)
        org.locationtech.jts.geom.Point jtsCentroid = multiPolygon.getCentroid();
        // Convert JTS Point (x=lon, y=lat) to Spring Data Point
        Point springCenter = new Point(jtsCentroid.getX(), jtsCentroid.getY());

        // 5. Create and Save Entity
        RegionEntity region = RegionEntity.builder()
                .name(name)
                .iso(iso)
                .geometry(multiPolygon) // Will be converted to WKT string by the Converter
                .center(springCenter)   // Native Neo4j Point
                .build();

        regionRepository.save(region);
        log.info("Saved Region: {}", name);
    }

    // Helper to parse [[[ [x,y], [x,y] ]]] structure
    private MultiPolygon parseMultiPolygon(JsonNode coordinatesNode) {
        List<Polygon> polygons = new ArrayList<>();

        for (JsonNode polygonNode : coordinatesNode) {
            // A polygon has rings: index 0 is exterior, others are holes
            LinearRing shell = null;
            List<LinearRing> holes = new ArrayList<>();

            for (int i = 0; i < polygonNode.size(); i++) {
                JsonNode ringNode = polygonNode.get(i);
                Coordinate[] coordinates = parseCoordinates(ringNode);
                LinearRing ring = geometryFactory.createLinearRing(coordinates);

                if (i == 0) {
                    shell = ring;
                } else {
                    holes.add(ring);
                }
            }

            if (shell != null) {
                polygons.add(geometryFactory.createPolygon(shell, holes.toArray(new LinearRing[0])));
            }
        }
        return geometryFactory.createMultiPolygon(polygons.toArray(new Polygon[0]));
    }

    private Coordinate[] parseCoordinates(JsonNode ringNode) {
        List<Coordinate> coords = new ArrayList<>();
        for (JsonNode coordNode : ringNode) {
            double lon = coordNode.get(0).asDouble();
            double lat = coordNode.get(1).asDouble();
            coords.add(new Coordinate(lon, lat));
        }
        return coords.toArray(new Coordinate[0]);
    }

}
