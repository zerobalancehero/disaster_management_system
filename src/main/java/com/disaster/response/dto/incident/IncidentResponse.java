package com.disaster.response.dto.incident;

import com.disaster.response.domain.entity.Incident;
import com.disaster.response.domain.enums.DisasterType;
import com.disaster.response.domain.enums.IncidentSeverity;
import com.disaster.response.domain.enums.VerificationStatus;
import com.disaster.response.dto.common.GeoPointDto;
import com.disaster.response.gis.GisUtil;
import com.fasterxml.jackson.annotation.JsonProperty;

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
    @JsonProperty("lat")
    public Double lat() {
        return location != null ? location.latitude() : null;
    }

    @JsonProperty("lon")
    public Double lon() {
        return location != null ? location.longitude() : null;
    }

    @JsonProperty("latitude")
    public Double latitude() {
        return location != null ? location.latitude() : null;
    }

    @JsonProperty("longitude")
    public Double longitude() {
        return location != null ? location.longitude() : null;
    }

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
