package com.disaster.response.gis;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;

/**
 * Spatial / GIS Utilities for SRID 4326 (WGS 84).
 * IMPORTANT COORDINATE CONVENTION:
 * JTS / PostGIS geometries use (X, Y) which corresponds to (Longitude, Latitude).
 * Latitude is [-90.0, 90.0] and Longitude is [-180.0, 180.0].
 */
public final class GisUtil {

    public static final int SRID_WGS84 = 4326;
    private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory(new PrecisionModel(), SRID_WGS84);
    private static final double EARTH_RADIUS_KM = 6371.0088;

    private GisUtil() {
        // utility class
    }

    public static GeometryFactory getGeometryFactory() {
        return GEOMETRY_FACTORY;
    }

    /**
     * Create a JTS Point from latitude and longitude.
     * Note: JTS Coordinate expects (x, y) = (longitude, latitude).
     */
    public static Point createPoint(double latitude, double longitude) {
        validateCoordinates(latitude, longitude);
        Point point = GEOMETRY_FACTORY.createPoint(new Coordinate(longitude, latitude));
        point.setSRID(SRID_WGS84);
        return point;
    }

    public static void validateCoordinates(double latitude, double longitude) {
        if (latitude < -90.0 || latitude > 90.0) {
            throw new IllegalArgumentException("Latitude must be between -90.0 and 90.0 degrees. Provided: " + latitude);
        }
        if (longitude < -180.0 || longitude > 180.0) {
            throw new IllegalArgumentException("Longitude must be between -180.0 and 180.0 degrees. Provided: " + longitude);
        }
    }

    /**
     * Calculates great-circle distance between two Points in kilometers using Haversine formula.
     */
    public static double haversineDistanceKm(Point p1, Point p2) {
        if (p1 == null || p2 == null) {
            return 0.0;
        }
        // p.getY() is Latitude, p.getX() is Longitude
        return haversineDistanceKm(p1.getY(), p1.getX(), p2.getY(), p2.getX());
    }

    /**
     * Calculates distance between two lat/lon pairs in kilometers using Haversine formula.
     */
    public static double haversineDistanceKm(double lat1, double lon1, double lat2, double lon2) {
        validateCoordinates(lat1, lon1);
        validateCoordinates(lat2, lon2);

        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return EARTH_RADIUS_KM * c;
    }

    public static double getLatitude(Point point) {
        return point != null ? point.getY() : 0.0;
    }

    public static double getLongitude(Point point) {
        return point != null ? point.getX() : 0.0;
    }
}
