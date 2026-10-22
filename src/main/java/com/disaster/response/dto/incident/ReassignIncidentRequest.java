package com.disaster.response.dto.incident;

import jakarta.validation.constraints.NotBlank;

public record ReassignIncidentRequest(
    @NotBlank(message = "Reassignment reason is required")
    String reason
) {}
