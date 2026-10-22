package com.disaster.response.dto.incident;

import com.disaster.response.domain.entity.Incident;
import com.disaster.response.domain.enums.DisasterType;
import com.disaster.response.domain.enums.IncidentSeverity;
import com.disaster.response.domain.enums.VerificationStatus;
import com.disaster.response.dto.common.GeoPointDto;
import com.disaster.response.gis.GisUtil;

import java.time.Instant;
import java.util.UUID;

public record IncidentResponse(
    UUID id,
    UUID reporterId,
    String reporterName,
    String title,
    String description,
    DisasterType disasterType,
    IncidentSeverity severity,
    GeoPointDto location,
    Instant reportedTime,
    VerificationStatus verificationStatus,
    UUID verifierId,
    String verifierName,
    Instant verificationTime,
    String rejectionReason,
    Instant createdAt,
    Instant updatedAt
) {
    public static IncidentResponse from(Incident incident) {
        if (incident == null) {
            return null;
        }
        GeoPointDto loc = incident.getLocation() != null
                ? GeoPointDto.of(GisUtil.getLatitude(incident.getLocation()), GisUtil.getLongitude(incident.getLocation()))
                : null;

        return new IncidentResponse(
            incident.getId(),
            incident.getReporter() != null ? incident.getReporter().getId() : null,
            incident.getReporter() != null ? incident.getReporter().getName() : null,
            incident.getTitle(),
            incident.getDescription(),
            incident.getDisasterType(),
            incident.getSeverity(),
            loc,
            incident.getReportedTime(),
            incident.getVerificationStatus(),
            incident.getVerifier() != null ? incident.getVerifier().getId() : null,
            incident.getVerifier() != null ? incident.getVerifier().getName() : null,
            incident.getVerificationTime(),
            incident.getRejectionReason(),
            incident.getCreatedAt(),
            incident.getUpdatedAt()
        );
    }
}
