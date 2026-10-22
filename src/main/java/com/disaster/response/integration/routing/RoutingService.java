package com.disaster.response.integration.routing;

import org.locationtech.jts.geom.Point;

public interface RoutingService {

    record RoutingResult(
        double distanceKm,
        int estimatedDurationMinutes,
        String provider,
        boolean isRoadRouting
    ) {}

    RoutingResult calculateRoute(Point start, Point destination);
}
