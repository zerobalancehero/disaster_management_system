package com.disaster.response.dto.incident;

import com.disaster.response.domain.enums.DisasterType;
import com.disaster.response.domain.enums.IncidentSeverity;
import com.disaster.response.dto.common.GeoPointDto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record IncidentReportRequest(
    @NotBlank(message = "Title is required")
    @Size(max = 255, message = "Title must not exceed 255 characters")
    String title,

    @NotBlank(message = "Description is required")
    String description,

    @NotNull(message = "Disaster type is required")
    DisasterType disasterType,

    @NotNull(message = "Severity is required")
    IncidentSeverity severity,

    @NotNull(message = "Location is required")
    @Valid
    GeoPointDto location
) {}
