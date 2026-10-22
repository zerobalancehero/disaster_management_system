package com.disaster.response.dto.report;

import java.util.Map;

public record AdminDashboardResponse(
    long totalIncidents,
    long pendingVerificationIncidents,
    long verifiedIncidents,
    long rejectedIncidents,
    Map<String, Long> incidentsBySeverity,
    long activeDisasterAlerts,
    long totalResponderTeams,
    long availableTeams,
    long deployedTeams,
    long activeAssignments,
    long totalUsers,
    String modelStatus,
    String dataFreshnessStatus
) {}
