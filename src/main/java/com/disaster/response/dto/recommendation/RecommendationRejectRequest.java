package com.disaster.response.dto.recommendation;

import jakarta.validation.constraints.NotBlank;

public record RecommendationRejectRequest(
    @NotBlank(message = "Rejection reason is required")
    String rejectionReason
) {}
