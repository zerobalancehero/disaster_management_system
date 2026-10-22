package com.disaster.response.dto.risk;

import com.disaster.response.domain.entity.RiskAssessment;
import com.disaster.response.domain.enums.DisasterType;
import com.disaster.response.domain.enums.PredictionHorizon;
import com.disaster.response.domain.enums.RiskCategory;

import java.time.Instant;
import java.util.UUID;

public record RiskAssessmentResponse(
    UUID id,
    UUID regionId,
    String regionName,
    DisasterType disasterType,
    double probability,
    RiskCategory riskCategory,
    PredictionHorizon predictionHorizon,
    Instant predictionTimestamp,
    String modelVersion,
    boolean isSynthetic
) {
    public static RiskAssessmentResponse from(RiskAssessment ra) {
        if (ra == null) {
            return null;
        }
        boolean synthetic = ra.getModelVersion() != null && ra.getModelVersion().contains("MOCK");
        return new RiskAssessmentResponse(
            ra.getId(),
            ra.getRegion() != null ? ra.getRegion().getId() : null,
            ra.getRegion() != null ? ra.getRegion().getName() : null,
            ra.getDisasterType(),
            ra.getProbability(),
            ra.getRiskCategory(),
            ra.getPredictionHorizon(),
            ra.getPredictionTimestamp(),
            ra.getModelVersion(),
            synthetic
        );
    }
}
