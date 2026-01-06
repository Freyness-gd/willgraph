package at.ac.tuwien.mogda.willgraph.config;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;
import org.locationtech.jts.io.WKTWriter;
import org.neo4j.driver.Value;
import org.neo4j.driver.Values;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.data.convert.WritingConverter;
import org.springframework.data.neo4j.core.convert.Neo4jConversions;

import java.util.List;

@Configuration
public class Neo4jConfig {

    @Bean
    public Neo4jConversions neo4jConversions() {
        return new Neo4jConversions(List.of(
                new MultiPolygonToValueConverter(),
                new GeometryToValueConverter(),
                new ValueToGeometryConverter()
        ));
    }

    @WritingConverter
    static class MultiPolygonToValueConverter implements Converter<MultiPolygon, Value> {
        @Override
        public Value convert(MultiPolygon source) {
            return Values.value(new WKTWriter().write(source));
        }
    }

    @WritingConverter
    static class GeometryToValueConverter implements Converter<Geometry, Value> {
        @Override
        public Value convert(Geometry source) {
            return Values.value(new WKTWriter().write(source));
        }
    }

    @ReadingConverter
    static class ValueToGeometryConverter implements Converter<Value, Geometry> {
        @Override
        public Geometry convert(Value source) {
            // Handle nulls from the database
            if (source == null || source.isNull()) {
                return null;
            }
            // The driver returns a Value object; we extract the String (WKT) from it
            String wkt = source.asString();
            try {
                return new WKTReader().read(wkt);
            } catch (ParseException e) {
                throw new IllegalArgumentException("Failed to parse WKT", e);
            }
        }
    }
}
