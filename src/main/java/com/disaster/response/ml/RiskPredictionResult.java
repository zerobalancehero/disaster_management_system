package com.disaster.response.ml;

import com.disaster.response.domain.enums.DisasterType;
import com.disaster.response.domain.enums.PredictionHorizon;
import com.disaster.response.domain.enums.RiskCategory;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record RiskPredictionResult(
    UUID regionId,
    DisasterType disasterType,
    double probability,
    RiskCategory riskCategory,
    PredictionHorizon predictionHorizon,
    Instant timestamp,
    String modelVersion,
    String framework,
    Map<String, Object> featureSnapshot,
    Map<String, Object> inferenceMetadata
) {}
