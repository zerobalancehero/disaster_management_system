package com.disaster.response.service;

import com.disaster.response.domain.entity.Region;
import com.disaster.response.domain.entity.RiskAssessment;
import com.disaster.response.domain.entity.TerrainFeature;
import com.disaster.response.domain.enums.DisasterType;
import com.disaster.response.domain.enums.PredictionHorizon;
import com.disaster.response.dto.risk.RegionRiskResponse;
import com.disaster.response.dto.risk.RiskAssessmentResponse;
import com.disaster.response.dto.risk.RiskModelInfoResponse;
import com.disaster.response.exception.ResourceNotFoundException;
import com.disaster.response.gis.TerrainFeatureService;
import com.disaster.response.ml.DisasterRiskPredictor;
import com.disaster.response.ml.RiskFeatureVector;
import com.disaster.response.ml.RiskPredictionResult;
import com.disaster.response.repository.ModelRegistryRepository;
import com.disaster.response.repository.RegionRepository;
import com.disaster.response.repository.RiskAssessmentRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class RiskAssessmentService {

    private static final Logger log = LoggerFactory.getLogger(RiskAssessmentService.class);

    private final RiskAssessmentRepository riskAssessmentRepository;
    private final RegionRepository regionRepository;
    private final TerrainFeatureService terrainFeatureService;
    private final DisasterRiskPredictor predictor;
    private final ModelRegistryRepository modelRegistryRepository;
    private final ObjectMapper objectMapper;

    public RiskAssessmentService(
            RiskAssessmentRepository riskAssessmentRepository,
            RegionRepository regionRepository,
            TerrainFeatureService terrainFeatureService,
            DisasterRiskPredictor predictor,
            ModelRegistryRepository modelRegistryRepository,
            ObjectMapper objectMapper) {
        this.riskAssessmentRepository = riskAssessmentRepository;
        this.regionRepository = regionRepository;
        this.terrainFeatureService = terrainFeatureService;
        this.predictor = predictor;
        this.modelRegistryRepository = modelRegistryRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public RiskAssessmentResponse calculateAndSaveRisk(UUID regionId, DisasterType disasterType, PredictionHorizon horizon) {
        Region region = regionRepository.findById(regionId)
                .orElseThrow(() -> new ResourceNotFoundException("Region not found with ID: " + regionId));

        Optional<TerrainFeature> terrainOpt = terrainFeatureService.getFeaturesForRegion(regionId);

        RiskFeatureVector vector = new RiskFeatureVector(
                regionId,
                disasterType,
                horizon,
                28.5, // Current or forecast temp
                78.0, // Current or forecast humidity
                45.0, // Current or forecast rainfall
                25.0, // Wind speed
                terrainOpt.map(TerrainFeature::getElevation).orElse(50.0),
                terrainOpt.map(TerrainFeature::getSlope).orElse(5.0),
                terrainOpt.map(TerrainFeature::getFlowAccumulation).orElse(null),
                terrainOpt.map(TerrainFeature::getDistanceToRiver).orElse(null)
        );

        RiskPredictionResult result = predictor.predict(vector);

        String featureJson = null;
        String metaJson = null;
        try {
            featureJson = objectMapper.writeValueAsString(result.featureSnapshot());
            metaJson = objectMapper.writeValueAsString(result.inferenceMetadata());
        } catch (Exception e) {
            log.warn("Failed to serialize feature metadata: {}", e.getMessage());
        }

        RiskAssessment assessment = new RiskAssessment(
                region,
                disasterType,
                result.probability(),
                result.riskCategory(),
                horizon,
                result.modelVersion(),
                featureJson,
                metaJson
        );

        RiskAssessment saved = riskAssessmentRepository.save(assessment);
        log.info("Saved risk assessment {} for region {} (probability={}, category={})",
                saved.getId(), region.getName(), saved.getProbability(), saved.getRiskCategory());

        return RiskAssessmentResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public List<RegionRiskResponse> getAllRegionsWithLatestRisk() {
        List<Region> regions = regionRepository.findAll();
        return regions.stream().map(region -> {
            List<RiskAssessmentResponse> risks = riskAssessmentRepository
                    .findByRegionIdOrderByPredictionTimestampDesc(region.getId())
                    .stream()
                    .limit(5)
                    .map(RiskAssessmentResponse::from)
                    .toList();
            return RegionRiskResponse.of(region, risks);
        }).toList();
    }

    @Transactional(readOnly = true)
    public RegionRiskResponse getRegionRisk(UUID regionId) {
        Region region = regionRepository.findById(regionId)
                .orElseThrow(() -> new ResourceNotFoundException("Region not found with ID: " + regionId));

        List<RiskAssessmentResponse> risks = riskAssessmentRepository
                .findByRegionIdOrderByPredictionTimestampDesc(regionId)
                .stream()
                .limit(10)
                .map(RiskAssessmentResponse::from)
                .toList();

        return RegionRiskResponse.of(region, risks);
    }

    @Transactional(readOnly = true)
    public Page<RiskAssessmentResponse> getRiskHistory(UUID regionId, Pageable pageable) {
        return riskAssessmentRepository.findByRegionIdOrderByPredictionTimestampDesc(regionId, pageable)
                .map(RiskAssessmentResponse::from);
    }

    @Transactional(readOnly = true)
    public RiskModelInfoResponse getModelInfo() {
        return modelRegistryRepository.findByActiveTrue()
                .map(m -> RiskModelInfoResponse.from(m, "DETERMINISTIC_MOCK".equals(predictor.getFramework())))
                .orElseGet(() -> new RiskModelInfoResponse(
                        null,
                        "DisasterRiskPredictor",
                        predictor.getModelVersion(),
                        predictor.getFramework(),
                        predictor.isAvailable(),
                        RiskFeatureVector.SCHEMA_VERSION,
                        java.time.Instant.now(),
                        "Inference adapter registered",
                        "DETERMINISTIC_MOCK".equals(predictor.getFramework())
                ));
    }
}
