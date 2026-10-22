package com.disaster.response;

import com.disaster.response.gis.GisUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;

import static org.junit.jupiter.api.Assertions.*;

class GisAndSpatialCalculationTest {

    @Test
    @DisplayName("Coordinate Validation: Latitude must be between -90 and 90; Longitude between -180 and 180")
    void testCoordinateValidation() {
        assertDoesNotThrow(() -> GisUtil.validateCoordinates(13.0827, 80.2707)); // Chennai

        assertThrows(IllegalArgumentException.class, () -> GisUtil.validateCoordinates(95.0, 80.0));
        assertThrows(IllegalArgumentException.class, () -> GisUtil.validateCoordinates(-95.0, 80.0));
        assertThrows(IllegalArgumentException.class, () -> GisUtil.validateCoordinates(13.0, 185.0));
        assertThrows(IllegalArgumentException.class, () -> GisUtil.validateCoordinates(13.0, -185.0));
    }

    @Test
    @DisplayName("SRID 4326 Point Creation: Ensures correct (longitude, latitude) order in JTS Coordinate")
    void testPointCreationAndSRID() {
        double lat = 13.0827;
        double lon = 80.2707;
        Point point = GisUtil.createPoint(lat, lon);

        assertNotNull(point);
        assertEquals(4326, point.getSRID());
        // In JTS: X is Longitude, Y is Latitude
        assertEquals(lon, point.getX(), 0.0001);
        assertEquals(lat, point.getY(), 0.0001);
        assertEquals(lat, GisUtil.getLatitude(point), 0.0001);
        assertEquals(lon, GisUtil.getLongitude(point), 0.0001);
    }

    @Test
    @DisplayName("Haversine Distance: Accurate great-circle distance between two geographic points")
    void testHaversineDistance() {
        // Chennai Central (13.0827, 80.2755) to Chennai Airport (12.9941, 80.1709) ~ 14.8 km straight line
        Point p1 = GisUtil.createPoint(13.0827, 80.2755);
        Point p2 = GisUtil.createPoint(12.9941, 80.1709);

        double distance = GisUtil.haversineDistanceKm(p1, p2);

        assertTrue(distance > 13.0 && distance < 17.0,
                "Distance should be approximately 14.8 km, but was: " + distance);
    }

    @Test
    @DisplayName("Spatial Containment: Point inside buffer returns true; point outside returns false")
    void testSpatialBufferContainment() {
        Point epicenter = GisUtil.createPoint(13.00, 80.00);
        // Create 0.1 degree buffer (~11 km)
        Geometry buffer = epicenter.buffer(0.10);

        Point nearbyPoint = GisUtil.createPoint(13.02, 80.02); // Inside buffer
        Point farPoint = GisUtil.createPoint(13.50, 80.50);    // Far outside buffer

        assertTrue(buffer.contains(nearbyPoint), "Nearby point should be inside the disaster buffer");
        assertFalse(buffer.contains(farPoint), "Far point should be outside the disaster buffer");
    }
}
