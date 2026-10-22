package com.disaster.response.dto.assignment;

import com.disaster.response.domain.enums.AssignmentStatus;
import com.disaster.response.dto.common.GeoPointDto;
import jakarta.validation.constraints.NotNull;

public record UpdateAssignmentStatusRequest(
    @NotNull(message = "Target status is required")
    AssignmentStatus status,

    String message,

    GeoPointDto location
) {}
