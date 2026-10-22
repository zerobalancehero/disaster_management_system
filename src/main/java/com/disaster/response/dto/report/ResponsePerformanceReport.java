package com.disaster.response.dto.report;

import java.util.Map;

public record ResponsePerformanceReport(
    long totalAssignments,
    long resolvedAssignments,
    long cancelledAssignments,
    long inProgressAssignments,
    Double averageDispatchDurationMinutes,
    Double averageResolutionDurationHours,
    Map<String, Long> assignmentsByTeam,
    Map<String, Long> assignmentsByStatus
) {}
