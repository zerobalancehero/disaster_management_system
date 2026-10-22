package com.disaster.response.dto.recommendation;

import com.disaster.response.domain.entity.AssignmentRecommendation;
import com.disaster.response.domain.enums.RecommendationStatus;

import java.time.Instant;
import java.util.UUID;

public record RecommendationResponse(
    UUID id,
    UUID incidentId,
    UUID teamId,
    String teamName,
    int rank,
    Integer estimatedTravelTimeMinutes,
    boolean eligibility,
    String availabilitySnapshot,
    String recommendationReason,
    Instant generationTime,
    RecommendationStatus recommendationStatus
) {
    public static RecommendationResponse from(AssignmentRecommendation rec) {
        if (rec == null) {
            return null;
        }
        return new RecommendationResponse(
            rec.getId(),
            rec.getIncident() != null ? rec.getIncident().getId() : null,
            rec.getTeam() != null ? rec.getTeam().getId() : null,
            rec.getTeam() != null ? rec.getTeam().getName() : null,
            rec.getRank(),
            rec.getEstimatedTravelTimeMinutes(),
            rec.isEligibility(),
            rec.getAvailabilitySnapshot(),
            rec.getRecommendationReason(),
            rec.getGenerationTime(),
            rec.getRecommendationStatus()
        );
    }
}
