package com.disaster.response.dto.incident;

import com.disaster.response.domain.enums.IncidentSeverity;

public record VerifyIncidentRequest(
    IncidentSeverity severityOverride,
    String comments
) {}
