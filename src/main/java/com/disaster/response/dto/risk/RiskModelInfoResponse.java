package com.disaster.response.dto.risk;

import com.disaster.response.domain.entity.ModelRegistry;
import java.time.Instant;
import java.util.UUID;

public record RiskModelInfoResponse(
    UUID id,
    String modelName,
    String version,
    String framework,
    boolean active,
    String featureSchemaVersion,
    Instant createdAt,
    String evaluationMetadata,
    boolean mockPredictorActive
) {
    public static RiskModelInfoResponse from(ModelRegistry mr, boolean mockActive) {
        if (mr == null) {
            return new RiskModelInfoResponse(
                null,
                "No Model Registered",
                "0.0.0",
                "NONE",
                false,
                "v1.0",
                Instant.now(),
                null,
                mockActive
            );
        }
        return new RiskModelInfoResponse(
            mr.getId(),
            mr.getModelName(),
            mr.getVersion(),
            mr.getFramework(),
            mr.isActive(),
            mr.getFeatureSchemaVersion(),
            mr.getCreatedAt(),
            mr.getEvaluationMetadata(),
            mockActive
        );
    }
}
