package com.disaster.response.gis;

import com.disaster.response.domain.entity.Region;
import com.disaster.response.domain.entity.TerrainFeature;
import com.disaster.response.exception.ResourceNotFoundException;
import com.disaster.response.repository.RegionRepository;
import com.disaster.response.repository.TerrainFeatureRepository;
import org.locationtech.jts.geom.Point;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class DatabaseTerrainFeatureService implements TerrainFeatureService {

    private static final Logger log = LoggerFactory.getLogger(DatabaseTerrainFeatureService.class);

    private final TerrainFeatureRepository terrainFeatureRepository;
    private final RegionRepository regionRepository;

    public DatabaseTerrainFeatureService(TerrainFeatureRepository terrainFeatureRepository, RegionRepository regionRepository) {
        this.terrainFeatureRepository = terrainFeatureRepository;
        this.regionRepository = regionRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Double> getElevationForPoint(Point point) {
        if (point == null) {
            return Optional.empty();
        }
        List<Region> containingRegions = regionRepository.findRegionsContainingPoint(point);
        if (containingRegions.isEmpty()) {
            log.debug("No region found containing point ({}, {})", point.getY(), point.getX());
            return Optional.empty();
        }

        Region region = containingRegions.get(0);
        return terrainFeatureRepository.findFirstByRegionIdOrderByProcessingTimeDesc(region.getId())
                .map(TerrainFeature::getElevation);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<TerrainFeature> getFeaturesForRegion(UUID regionId) {
        return terrainFeatureRepository.findFirstByRegionIdOrderByProcessingTimeDesc(regionId);
    }

    @Override
    @Transactional
    public TerrainFeature saveTerrainFeatures(UUID regionId, double elevation, Double slope,
                                             Double flowAccumulation, Double distanceToRiver, String dataSource) {
        Region region = regionRepository.findById(regionId)
                .orElseThrow(() -> new ResourceNotFoundException("Region not found with ID: " + regionId));

        TerrainFeature feature = new TerrainFeature(region, elevation, slope, flowAccumulation, distanceToRiver, dataSource);
        feature.setProcessingTime(Instant.now());
        return terrainFeatureRepository.save(feature);
    }
}
