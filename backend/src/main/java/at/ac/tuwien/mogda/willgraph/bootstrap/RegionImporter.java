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
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.data.geo.Point;
import org.springframework.stereotype.Component;

import java.io.IOException;
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

        log.info("Starting Region Import...");

        // 1. Load all JSON files from the 'regions' folder in classpath
        ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        Resource[] resources = resolver.getResources("classpath*:regions/*.json");

        if (resources.length == 0) {
            log.warn("No region files found in 'src/main/resources/regions/'");
            return;
        }

        for (Resource resource : resources) {
            try (InputStream inputStream = resource.getInputStream()) {
                log.info("Processing file: {}", resource.getFilename());
                JsonNode root = objectMapper.readTree(inputStream);
                JsonNode features = root.get("features");

                if (features != null && features.isArray()) {
                    for (JsonNode feature : features) {
                        processFeature(feature);
                    }
                }
            } catch (Exception e) {
                log.error("Failed to parse region file: {}", resource.getFilename(), e);
            }
        }

        log.info("Region import completed.");
    }

    private void processFeature(JsonNode feature) throws IOException {
        // 1. Extract Properties
        JsonNode properties = feature.get("properties");
        // Use default values to prevent NullPointerExceptions
        String name = properties.has("name") ? properties.get("name").asText() : "Unknown";
        String iso = properties.has("iso") ? properties.get("iso").asText() : null;

        // 2. Extract Geometry
        JsonNode geometryNode = feature.get("geometry");
        if (geometryNode == null) return;

        String type = geometryNode.get("type").asText();

        // 3. Parse & Normalize to MultiPolygon
        MultiPolygon multiPolygon = null;

        if ("MultiPolygon".equalsIgnoreCase(type)) {
            multiPolygon = parseMultiPolygon(geometryNode.get("coordinates"));
        } else if ("Polygon".equalsIgnoreCase(type)) {
            // Wrap single Polygon in a MultiPolygon for consistency
            Polygon singlePolygon = parsePolygon(geometryNode.get("coordinates"));
            multiPolygon = geometryFactory.createMultiPolygon(new Polygon[]{singlePolygon});
        } else {
            log.warn("Skipping region {} - Unsupported geometry type: {}", name, type);
            return;
        }

        if (multiPolygon == null) return;

        // 4. Calculate Center (Centroid)
        org.locationtech.jts.geom.Point jtsCentroid = multiPolygon.getCentroid();
        Point springCenter = new Point(jtsCentroid.getX(), jtsCentroid.getY());

        // 5. Create and Save Entity
        RegionEntity region = RegionEntity.builder()
                .name(name)
                .iso(iso)
                .geometry(multiPolygon)
                .center(springCenter)
                .build();

        regionRepository.save(region);
    }

    private MultiPolygon parseMultiPolygon(JsonNode coordinatesNode) {
        List<Polygon> polygons = new ArrayList<>();
        for (JsonNode polygonNode : coordinatesNode) {
            polygons.add(parsePolygon(polygonNode));
        }
        return geometryFactory.createMultiPolygon(polygons.toArray(new Polygon[0]));
    }

    private Polygon parsePolygon(JsonNode polygonNode) {
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

        if (shell == null) return null;
        return geometryFactory.createPolygon(shell, holes.toArray(new LinearRing[0]));
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
