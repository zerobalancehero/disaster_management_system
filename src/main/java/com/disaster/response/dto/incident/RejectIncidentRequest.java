package com.disaster.response.dto.incident;

import jakarta.validation.constraints.NotBlank;

public record RejectIncidentRequest(
    @NotBlank(message = "Rejection reason is required")
    String rejectionReason
) {}
