package com.disaster.response.integration.alert;

import com.disaster.response.domain.entity.DisasterAlert;
import com.disaster.response.domain.enums.AlertSeverity;
import com.disaster.response.domain.enums.AlertStatus;
import com.disaster.response.domain.enums.DisasterType;
import com.disaster.response.gis.GisUtil;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Polygon;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * Live Alert Data Provider for GDACS (Global Disaster Alert and Coordination System).
 * Ingests live RSS/GeoRSS alerts for floods, cyclones, earthquakes, tsunamis and wildfires.
 */
@Component
public class GdacsAlertProvider implements AlertDataProvider {

    private static final Logger log = LoggerFactory.getLogger(GdacsAlertProvider.class);

    private final String baseUrl;
    private final boolean enabled;
    private final RestClient restClient;

    public GdacsAlertProvider(
            @Value("${app.integrations.gdacs.base-url:https://www.gdacs.org/xml/rss.xml}") String baseUrl,
            @Value("${app.integrations.gdacs.enabled:true}") boolean enabled,
            @Value("${app.integrations.gdacs.timeout-seconds:8}") int timeoutSeconds) {
        this.baseUrl = baseUrl;
        this.enabled = enabled;

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(timeoutSeconds * 1000);
        factory.setReadTimeout(timeoutSeconds * 1000);

        this.restClient = RestClient.builder()
                .requestFactory(factory)
                .build();
    }

    @Override
    public String getProviderName() {
        return "GDACS";
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
            String xmlContent = restClient.get()
                    .uri(baseUrl)
                    .retrieve()
                    .body(String.class);

            if (xmlContent == null || xmlContent.isBlank()) {
                return List.of();
            }

            xmlContent = xmlContent.trim();
            if (xmlContent.startsWith("\uFEFF")) {
                xmlContent = xmlContent.substring(1);
            }
            int xmlStart = xmlContent.indexOf("<?xml");
            if (xmlStart > 0) {
                xmlContent = xmlContent.substring(xmlStart);
            }

            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(true);
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.parse(new InputSource(new StringReader(xmlContent)));

            NodeList items = doc.getElementsByTagName("item");
            List<DisasterAlert> alerts = new ArrayList<>();

            for (int i = 0; i < items.getLength(); i++) {
                if (items.item(i) instanceof Element item) {
                    try {
                        DisasterAlert alert = parseItem(item);
                        if (alert != null) {
                            alerts.add(alert);
                        }
                    } catch (Exception e) {
                        log.debug("Skipping unparseable GDACS item {}: {}", i, e.getMessage());
                    }
                }
            }

            log.info("Successfully fetched {} disaster alerts from GDACS feed", alerts.size());
            return alerts;
        } catch (Exception e) {
            log.error("Failed to fetch disaster alerts from GDACS: {}", e.getMessage());
            return List.of();
        }
    }

    private DisasterAlert parseItem(Element item) {
        String guid = getTagText(item, "guid");
        if (guid == null || guid.isBlank()) {
            guid = getTagText(item, "link");
        }
        if (guid == null || guid.isBlank()) {
            return null;
        }

        String title = getTagText(item, "title");
        if (title != null && title.length() > 250) {
            title = title.substring(0, 247) + "...";
        }

        String description = getTagText(item, "description");
        if (description != null && description.length() > 2000) {
            description = description.substring(0, 1997) + "...";
        }
        String eventTypeStr = getTagText(item, "gdacs:eventtype");
        String alertLevelStr = getTagText(item, "gdacs:alertlevel");
        String pubDateStr = getTagText(item, "pubDate");

        Double lat = parseDouble(getTagText(item, "geo:lat"));
        Double lon = parseDouble(getTagText(item, "geo:long"));

        if (lat == null || lon == null) {
            String georssPoint = getTagText(item, "georss:point");
            if (georssPoint != null && !georssPoint.isBlank()) {
                String[] parts = georssPoint.trim().split("\\s+");
                if (parts.length >= 2) {
                    lat = parseDouble(parts[0]);
                    lon = parseDouble(parts[1]);
                }
            }
        }

        if (lat == null || lon == null) {
            return null;
        }

        DisasterType disasterType = mapDisasterType(eventTypeStr);
        AlertSeverity severity = mapAlertSeverity(alertLevelStr);

        Instant issueTime = Instant.now();
        if (pubDateStr != null && !pubDateStr.isBlank()) {
            try {
                issueTime = Instant.from(DateTimeFormatter.RFC_1123_DATE_TIME.parse(pubDateStr.trim()));
            } catch (Exception e) {
                log.debug("Could not parse pubDate '{}': {}", pubDateStr, e.getMessage());
            }
        }

        Instant expiryTime = issueTime.plus(3, ChronoUnit.DAYS);
        Geometry affectedArea = createCircularPolygon(lon, lat, 50.0);

        return new DisasterAlert(
                "GDACS",
                guid,
                disasterType,
                severity,
                title != null ? title : "GDACS Natural Disaster Alert",
                description,
                affectedArea,
                issueTime,
                expiryTime,
                AlertStatus.ACTIVE,
                item.getTextContent()
        );
    }

    private DisasterType mapDisasterType(String eventType) {
        if (eventType == null) return DisasterType.GENERAL_EMERGENCY;
        return switch (eventType.toUpperCase()) {
            case "EQ" -> DisasterType.EARTHQUAKE;
            case "TC" -> DisasterType.CYCLONE;
            case "FL" -> DisasterType.FLOOD;
            case "WF" -> DisasterType.WILDFIRE;
            case "LS" -> DisasterType.LANDSLIDE;
            case "TS" -> DisasterType.TSUNAMI;
            default -> DisasterType.GENERAL_EMERGENCY;
        };
    }

    private AlertSeverity mapAlertSeverity(String alertLevel) {
        if (alertLevel == null) return AlertSeverity.MODERATE;
        return switch (alertLevel.toLowerCase()) {
            case "red" -> AlertSeverity.EXTREME;
            case "orange" -> AlertSeverity.SEVERE;
            case "green" -> AlertSeverity.MODERATE;
            default -> AlertSeverity.LOW;
        };
    }

    private Geometry createCircularPolygon(double centerLon, double centerLat, double radiusKm) {
        int points = 32;
        Coordinate[] coords = new Coordinate[points + 1];
        double radiusDeg = radiusKm / 111.32;

        for (int i = 0; i < points; i++) {
            double angle = (2 * Math.PI * i) / points;
            double dx = radiusDeg * Math.cos(angle);
            double dy = radiusDeg * Math.sin(angle);
            coords[i] = new Coordinate(centerLon + dx, centerLat + dy);
        }
        coords[points] = coords[0];

        Polygon poly = GisUtil.getGeometryFactory().createPolygon(coords);
        poly.setSRID(GisUtil.SRID_WGS84);
        return poly;
    }

    private String getTagText(Element parent, String tagName) {
        NodeList nl = parent.getElementsByTagName(tagName);
        if (nl.getLength() > 0 && nl.item(0) != null) {
            return nl.item(0).getTextContent();
        }
        return null;
    }

    private Double parseDouble(String str) {
        if (str == null || str.isBlank()) return null;
        try {
            return Double.parseDouble(str.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}

