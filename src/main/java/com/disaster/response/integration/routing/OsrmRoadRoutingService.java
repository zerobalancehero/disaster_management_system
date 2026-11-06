package com.disaster.response.integration.routing;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.locationtech.jts.geom.Point;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Locale;

/**
 * Real Road Network Routing Service powered by OSRM (Open Source Routing Machine).
 * Calculates actual drivable road route distance and travel duration between responder squads and incident locations.
 * Automatically falls back to Haversine straight-line estimation if OSRM is offline or unreachable.
 */
@Service
@Primary
public class OsrmRoadRoutingService implements RoutingService {

    private static final Logger log = LoggerFactory.getLogger(OsrmRoadRoutingService.class);

    private final String baseUrl;
    private final boolean enabled;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final HaversineRoutingService fallbackService;

    public OsrmRoadRoutingService(
            @Value("${app.integrations.osrm.base-url:http://router.project-osrm.org}") String baseUrl,
            @Value("${app.integrations.osrm.enabled:true}") boolean enabled,
            @Value("${app.integrations.osrm.timeout-seconds:4}") int timeoutSeconds,
            ObjectMapper objectMapper,
            HaversineRoutingService fallbackService) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.enabled = enabled;
        this.objectMapper = objectMapper;
        this.fallbackService = fallbackService;

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(timeoutSeconds * 1000);
        factory.setReadTimeout(timeoutSeconds * 1000);

        this.restClient = RestClient.builder()
                .requestFactory(factory)
                .build();
    }

    @Override
    public RoutingResult calculateRoute(Point start, Point destination) {
        if (start == null || destination == null) {
            return new RoutingResult(0.0, 0, "HAVERSINE_ESTIMATE", false);
        }

        if (!enabled) {
            return fallbackService.calculateRoute(start, destination);
        }

        // OSRM format: /route/v1/driving/{lon1},{lat1};{lon2},{lat2}?overview=false
        double startLon = start.getX();
        double startLat = start.getY();
        double destLon = destination.getX();
        double destLat = destination.getY();

        String directUrl = String.format(Locale.ROOT,
                "%s/route/v1/driving/%.6f,%.6f;%.6f,%.6f?overview=false",
                baseUrl, startLon, startLat, destLon, destLat);

        try {
            String response = restClient.get()
                    .uri(directUrl)
                    .retrieve()
                    .body(String.class);

            if (response != null && !response.isBlank()) {
                JsonNode root = objectMapper.readTree(response);
                if ("Ok".equalsIgnoreCase(root.path("code").asText())) {
                    JsonNode routes = root.path("routes");
                    if (routes.isArray() && !routes.isEmpty()) {
                        JsonNode primaryRoute = routes.get(0);
                        double distanceMeters = primaryRoute.path("distance").asDouble(0.0);
                        double durationSeconds = primaryRoute.path("duration").asDouble(0.0);

                        double roadDistanceKm = Math.round((distanceMeters / 1000.0) * 10.0) / 10.0;
                        int durationMinutes = Math.max(1, (int) Math.round(durationSeconds / 60.0));

                        log.debug("OSRM road calculation successful: {} km, {} mins", roadDistanceKm, durationMinutes);
                        return new RoutingResult(roadDistanceKm, durationMinutes, "OSRM_ROAD_NETWORK", true);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("OSRM road routing unavailable or timed out ({}); falling back to Haversine: {}", baseUrl, e.getMessage());
        }

        return fallbackService.calculateRoute(start, destination);
    }
}
