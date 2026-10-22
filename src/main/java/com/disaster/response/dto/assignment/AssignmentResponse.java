package com.disaster.response.dto.assignment;

import com.disaster.response.domain.entity.IncidentAssignment;
import com.disaster.response.domain.enums.AssignmentStatus;
import com.disaster.response.dto.common.GeoPointDto;
import com.disaster.response.gis.GisUtil;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AssignmentResponse(
    UUID id,
    UUID incidentId,
    String incidentTitle,
    GeoPointDto incidentLocation,
    UUID teamId,
    String teamName,
    UUID approvingAdminId,
    String approvingAdminName,
    Instant approvalTime,
    AssignmentStatus assignmentStatus,
    Instant assignmentTime,
    Instant completionTime,
    String rejectionOrCancellationReason,
    long version,
    List<ResponseUpdateDto> updates
) {
    public record ResponseUpdateDto(
        UUID id,
        UUID actorId,
        String actorName,
        String status,
        String message,
        GeoPointDto location,
        Instant timestamp
    ) {}

    public static AssignmentResponse from(IncidentAssignment a) {
        if (a == null) {
            return null;
        }

        GeoPointDto loc = (a.getIncident() != null && a.getIncident().getLocation() != null)
                ? GeoPointDto.of(GisUtil.getLatitude(a.getIncident().getLocation()), GisUtil.getLongitude(a.getIncident().getLocation()))
                : null;

        List<ResponseUpdateDto> updateDtos = a.getResponseUpdates() != null
                ? a.getResponseUpdates().stream().map(u -> new ResponseUpdateDto(
                        u.getId(),
                        u.getActor() != null ? u.getActor().getId() : null,
                        u.getActor() != null ? u.getActor().getName() : null,
                        u.getStatus(),
                        u.getMessage(),
                        u.getLocation() != null ? GeoPointDto.of(GisUtil.getLatitude(u.getLocation()), GisUtil.getLongitude(u.getLocation())) : null,
                        u.getTimestamp()
                )).toList()
                : List.of();

        return new AssignmentResponse(
            a.getId(),
            a.getIncident() != null ? a.getIncident().getId() : null,
            a.getIncident() != null ? a.getIncident().getTitle() : null,
            loc,
            a.getTeam() != null ? a.getTeam().getId() : null,
            a.getTeam() != null ? a.getTeam().getName() : null,
            a.getApprovingAdmin() != null ? a.getApprovingAdmin().getId() : null,
            a.getApprovingAdmin() != null ? a.getApprovingAdmin().getName() : null,
            a.getApprovalTime(),
            a.getAssignmentStatus(),
            a.getAssignmentTime(),
            a.getCompletionTime(),
            a.getRejectionOrCancellationReason(),
            a.getVersion(),
            updateDtos
        );
    }
}
