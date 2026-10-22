package com.disaster.response.dto.assignment;

import com.disaster.response.dto.common.GeoPointDto;
import jakarta.validation.constraints.NotBlank;

public record ResponseUpdateRequest(
    @NotBlank(message = "Status is required")
    String status,

    @NotBlank(message = "Message is required")
    String message,

    GeoPointDto location
) {}
