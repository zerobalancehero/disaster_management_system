package com.disaster.response.dto.team;

import com.disaster.response.domain.enums.AvailabilityStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateTeamStatusRequest(
    @NotNull(message = "Availability status is required")
    AvailabilityStatus availabilityStatus
) {}
