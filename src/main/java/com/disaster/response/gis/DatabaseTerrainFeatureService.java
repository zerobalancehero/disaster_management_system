package com.disaster.response.gis;

import com.disaster.response.domain.entity.Region;
import com.disaster.response.domain.entity.TerrainFeature;
import com.disaster.response.exception.ResourceNotFoundException;
import com.disaster.response.repository.RegionRepository;
import com.disaster.response.repository.TerrainFeatureRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.locationtech.jts.geom.Point;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

@Service
public class DatabaseTerrainFeatureService implements TerrainFeatureService {

    private static final Logger log = LoggerFactory.getLogger(DatabaseTerrainFeatureService.class);

    private final TerrainFeatureRepository terrainFeatureRepository;
    private final RegionRepository regionRepository;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public DatabaseTerrainFeatureService(
            TerrainFeatureRepository terrainFeatureRepository,
            RegionRepository regionRepository,
            ObjectMapper objectMapper) {
        this.terrainFeatureRepository = terrainFeatureRepository;
        this.regionRepository = regionRepository;
        this.objectMapper = objectMapper;

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3000);
        factory.setReadTimeout(3000);
        this.restClient = RestClient.builder().requestFactory(factory).build();
    }

    @Override
    @Transactional
    public Optional<Double> getElevationForPoint(Point point) {
        if (point == null) {
            return Optional.empty();
        }
        List<Region> containingRegions = regionRepository.findRegionsContainingPoint(point);
        if (!containingRegions.isEmpty()) {
            Region region = containingRegions.get(0);
            Optional<Double> dbElevation = terrainFeatureRepository
                    .findFirstByRegionIdOrderByProcessingTimeDesc(region.getId())
                    .map(TerrainFeature::getElevation);
            if (dbElevation.isPresent()) {
                return dbElevation;
            }
        }

        // Live topographic elevation lookup via Open-Meteo elevation raster model
        Double liveElev = fetchLiveElevation(point.getY(), point.getX());
        return Optional.ofNullable(liveElev);
    }

    @Override
    @Transactional
    public Optional<TerrainFeature> getFeaturesForRegion(UUID regionId) {
        Optional<TerrainFeature> cached = terrainFeatureRepository.findFirstByRegionIdOrderByProcessingTimeDesc(regionId);
        if (cached.isPresent()) {
            return cached;
        }

        // Auto-populate region terrain features using real elevation
        Optional<Region> regionOpt = regionRepository.findById(regionId);
        if (regionOpt.isPresent() && regionOpt.get().getCenterPoint() != null) {
            Point center = regionOpt.get().getCenterPoint();
            Double elevation = fetchLiveElevation(center.getY(), center.getX());
            if (elevation != null) {
                TerrainFeature feature = new TerrainFeature(
                        regionOpt.get(),
                        elevation,
                        4.5, // gentle regional topographic slope
                        null,
                        null,
                        "OPEN_METEO_ELEVATION_LIVE"
                );
                feature.setProcessingTime(Instant.now());
                try {
                    return Optional.of(terrainFeatureRepository.save(feature));
                } catch (Exception e) {
                    log.debug("Could not persist auto-fetched terrain feature: {}", e.getMessage());
                    return Optional.of(feature);
                }
            }
        }

        return Optional.empty();
    }

    private Double fetchLiveElevation(double lat, double lon) {
        try {
            String url = String.format(Locale.ROOT,
                    "https://api.open-meteo.com/v1/elevation?latitude=%.4f&longitude=%.4f", lat, lon);
            String res = restClient.get().uri(url).retrieve().body(String.class);
            if (res != null && !res.isBlank()) {
                JsonNode root = objectMapper.readTree(res);
                JsonNode arr = root.path("elevation");
                if (arr.isArray() && !arr.isEmpty()) {
                    double elevation = arr.get(0).asDouble();
                    log.info("Live elevation queried for ({}, {}): {} meters", lat, lon, elevation);
                    return elevation;
                }
            }
        } catch (Exception e) {
            log.warn("Live elevation query failed for ({}, {}): {}", lat, lon, e.getMessage());
        }
        return null;
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
