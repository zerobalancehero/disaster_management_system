package com.disaster.response.integration.alert;

import com.disaster.response.domain.entity.DisasterAlert;
import com.disaster.response.domain.enums.AlertSeverity;
import com.disaster.response.domain.enums.AlertStatus;
import com.disaster.response.domain.enums.DisasterType;
import com.disaster.response.gis.GisUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Polygon;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Component
public class UsgsEarthquakeAlertProvider implements AlertDataProvider {

    private static final Logger log = LoggerFactory.getLogger(UsgsEarthquakeAlertProvider.class);

    private final String baseUrl;
    private final boolean enabled;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public UsgsEarthquakeAlertProvider(
            @Value("${app.integrations.usgs.base-url:https://earthquake.usgs.gov/fdsnws/event/1/query}") String baseUrl,
            @Value("${app.integrations.usgs.enabled:true}") boolean enabled,
            @Value("${app.integrations.usgs.timeout-seconds:5}") int timeoutSeconds,
            ObjectMapper objectMapper) {
        this.baseUrl = baseUrl;
        this.enabled = enabled;
        this.objectMapper = objectMapper;

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(timeoutSeconds * 1000);
        factory.setReadTimeout(timeoutSeconds * 1000);

        this.restClient = RestClient.builder()
                .requestFactory(factory)
                .build();
    }

    @Override
    public String getProviderName() {
        return "USGS";
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public List<DisasterAlert> fetchLatestAlerts() {
        if (!enabled) {
            return List.of();
        }

        try {
            String url = baseUrl + "?format=geojson&minmagnitude=4.5&limit=15";
            String response = restClient.get()
                    .uri(url)
                    .retrieve()
                    .body(String.class);

            JsonNode root = objectMapper.readTree(response);
            JsonNode features = root.path("features");

            List<DisasterAlert> alerts = new ArrayList<>();

            for (JsonNode feature : features) {
                String eventId = feature.path("id").asText();
                JsonNode properties = feature.path("properties");
                JsonNode geometry = feature.path("geometry");
                JsonNode coordinates = geometry.path("coordinates");

                double lon = coordinates.get(0).asDouble();
                double lat = coordinates.get(1).asDouble();
                double mag = properties.path("mag").asDouble();
                String title = properties.path("title").asText("Earthquake Event");
                long timeEpoch = properties.path("time").asLong(System.currentTimeMillis());
                Instant issueTime = Instant.ofEpochMilli(timeEpoch);

                // Create a circular buffer around epicenter (approx 0.5 deg ~ 55 km radius)
                Geometry affectedArea = GisUtil.createPoint(lat, lon).buffer(0.5);
                affectedArea.setSRID(4326);

                AlertSeverity severity = mapMagnitudeToSeverity(mag);

                DisasterAlert alert = new DisasterAlert(
                        getProviderName(),
                        eventId,
                        DisasterType.EARTHQUAKE,
                        severity,
                        title,
                        String.format("Magnitude %.1f earthquake detected at coordinates (%.4f, %.4f)", mag, lat, lon),
                        affectedArea,
                        issueTime,
                        issueTime.plus(java.time.Duration.ofDays(2)),
                        AlertStatus.ACTIVE,
                        feature.toString()
                );
                alerts.add(alert);
            }

            return alerts;
        } catch (Exception e) {
            log.warn("Failed to fetch earthquake data from USGS: {}", e.getMessage());
            return List.of();
        }
    }

    private AlertSeverity mapMagnitudeToSeverity(double mag) {
        if (mag >= 7.0) return AlertSeverity.EXTREME;
        if (mag >= 6.0) return AlertSeverity.SEVERE;
        if (mag >= 5.0) return AlertSeverity.MODERATE;
        return AlertSeverity.LOW;
    }
}
