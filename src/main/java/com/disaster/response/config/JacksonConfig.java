package com.disaster.response.config;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.io.IOException;

@Configuration
public class JacksonConfig {

    private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory(new PrecisionModel(), 4326);

    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        SimpleModule jtsModule = new SimpleModule();
        jtsModule.addSerializer(Point.class, new JsonSerializer<>() {
            @Override
            public void serialize(Point value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
                if (value == null) {
                    gen.writeNull();
                    return;
                }
                gen.writeStartObject();
                gen.writeNumberField("latitude", value.getY());
                gen.writeNumberField("longitude", value.getX());
                gen.writeEndObject();
            }
        });

        jtsModule.addDeserializer(Point.class, new JsonDeserializer<>() {
            @Override
            public Point deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
                JsonNode node = p.getCodec().readTree(p);
                if (node == null || node.isNull()) {
                    return null;
                }
                double lat = node.has("latitude") ? node.get("latitude").asDouble() : 0.0;
                double lon = node.has("longitude") ? node.get("longitude").asDouble() : 0.0;
                Point point = GEOMETRY_FACTORY.createPoint(new Coordinate(lon, lat));
                point.setSRID(4326);
                return point;
            }
        });

        mapper.registerModule(jtsModule);
        return mapper;
    }
}
