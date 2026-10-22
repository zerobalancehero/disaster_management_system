package com.disaster.response.domain.entity;

import com.disaster.response.domain.enums.AlertSeverity;
import com.disaster.response.domain.enums.AlertStatus;
import com.disaster.response.domain.enums.DisasterType;
import jakarta.persistence.*;
import org.locationtech.jts.geom.Geometry;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "disaster_alerts", uniqueConstraints = {
    @UniqueConstraint(name = "uk_alert_source_external_id", columnNames = {"source", "external_source_id"})
})
public class DisasterAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String source;

    @Column(name = "external_source_id", nullable = false)
    private String externalSourceId;

    @Enumerated(EnumType.STRING)
    @Column(name = "disaster_type", nullable = false)
    private DisasterType disasterType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AlertSeverity severity;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "affected_area", columnDefinition = "geometry(Geometry, 4326)", nullable = false)
    private Geometry affectedArea;

    @Column(name = "issue_time", nullable = false)
    private Instant issueTime;

    @Column(name = "expiry_time")
    private Instant expiryTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AlertStatus status = AlertStatus.ACTIVE;

    @Column(name = "raw_payload", columnDefinition = "TEXT")
    private String rawPayload;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public DisasterAlert() {
    }

    public DisasterAlert(String source, String externalSourceId, DisasterType disasterType,
                         AlertSeverity severity, String title, String description,
                         Geometry affectedArea, Instant issueTime, Instant expiryTime,
                         AlertStatus status, String rawPayload) {
        this.source = source;
        this.externalSourceId = externalSourceId;
        this.disasterType = disasterType;
        this.severity = severity;
        this.title = title;
        this.description = description;
        this.affectedArea = affectedArea;
        this.issueTime = issueTime;
        this.expiryTime = expiryTime;
        this.status = status != null ? status : AlertStatus.ACTIVE;
        this.rawPayload = rawPayload;
        this.createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getExternalSourceId() {
        return externalSourceId;
    }

    public void setExternalSourceId(String externalSourceId) {
        this.externalSourceId = externalSourceId;
    }

    public DisasterType getDisasterType() {
        return disasterType;
    }

    public void setDisasterType(DisasterType disasterType) {
        this.disasterType = disasterType;
    }

    public AlertSeverity getSeverity() {
        return severity;
    }

    public void setSeverity(AlertSeverity severity) {
        this.severity = severity;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Geometry getAffectedArea() {
        return affectedArea;
    }

    public void setAffectedArea(Geometry affectedArea) {
        this.affectedArea = affectedArea;
    }

    public Instant getIssueTime() {
        return issueTime;
    }

    public void setIssueTime(Instant issueTime) {
        this.issueTime = issueTime;
    }

    public Instant getExpiryTime() {
        return expiryTime;
    }

    public void setExpiryTime(Instant expiryTime) {
        this.expiryTime = expiryTime;
    }

    public AlertStatus getStatus() {
        return status;
    }

    public void setStatus(AlertStatus status) {
        this.status = status;
    }

    public String getRawPayload() {
        return rawPayload;
    }

    public void setRawPayload(String rawPayload) {
        this.rawPayload = rawPayload;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
