package com.disaster.response.dto.alert;

import com.disaster.response.domain.entity.DisasterAlert;
import com.disaster.response.domain.enums.AlertSeverity;
import com.disaster.response.domain.enums.AlertStatus;
import com.disaster.response.domain.enums.DisasterType;

import java.time.Instant;
import java.util.UUID;

public record DisasterAlertResponse(
    UUID id,
    String source,
    String externalSourceId,
    DisasterType disasterType,
    AlertSeverity severity,
    String title,
    String description,
    Instant issueTime,
    Instant expiryTime,
    AlertStatus status,
    String affectedAreaWkt
) {
    public static DisasterAlertResponse from(DisasterAlert alert) {
        if (alert == null) {
            return null;
        }
        return new DisasterAlertResponse(
            alert.getId(),
            alert.getSource(),
            alert.getExternalSourceId(),
            alert.getDisasterType(),
            alert.getSeverity(),
            alert.getTitle(),
            alert.getDescription(),
            alert.getIssueTime(),
            alert.getExpiryTime(),
            alert.getStatus(),
            alert.getAffectedArea() != null ? alert.getAffectedArea().toText() : null
        );
    }
}
