package com.disaster.response.gis;

import com.disaster.response.domain.entity.TerrainFeature;
import org.locationtech.jts.geom.Point;

import java.util.Optional;
import java.util.UUID;

public interface TerrainFeatureService {

    /**
     * Looks up elevation for a specific point by resolving its containing region.
     */
    Optional<Double> getElevationForPoint(Point point);

    /**
     * Retrieves terrain features for a specific region.
     */
    Optional<TerrainFeature> getFeaturesForRegion(UUID regionId);

    /**
     * Imports or saves processed DEM terrain features for a region.
     */
    TerrainFeature saveTerrainFeatures(UUID regionId, double elevation, Double slope,
                                       Double flowAccumulation, Double distanceToRiver, String dataSource);
}
