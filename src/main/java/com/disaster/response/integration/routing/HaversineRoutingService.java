package com.disaster.response.integration.routing;

import com.disaster.response.gis.GisUtil;
import org.locationtech.jts.geom.Point;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

@Service
@Primary
public class HaversineRoutingService implements RoutingService {

    // Average emergency response road speed: 45 km/h
    private static final double AVERAGE_SPEED_KMH = 45.0;
    // Detour index / winding factor between straight line and road: ~1.3
    private static final double WINDING_FACTOR = 1.30;

    @Override
    public RoutingResult calculateRoute(Point start, Point destination) {
        if (start == null || destination == null) {
            return new RoutingResult(0.0, 0, "HAVERSINE_ESTIMATE", false);
        }

        double straightLineDistance = GisUtil.haversineDistanceKm(start, destination);
        double estimatedRoadDistance = straightLineDistance * WINDING_FACTOR;

        // Duration in hours = distance / speed -> in minutes:
        double hours = estimatedRoadDistance / AVERAGE_SPEED_KMH;
        int durationMinutes = Math.max(1, (int) Math.round(hours * 60.0));

        return new RoutingResult(
                Math.round(estimatedRoadDistance * 10.0) / 10.0,
                durationMinutes,
                "HAVERSINE_ESTIMATE",
                false
        );
    }
}
