package com.disaster.response.domain.entity;

import com.disaster.response.domain.enums.DisasterType;
import com.disaster.response.domain.enums.IncidentSeverity;
import com.disaster.response.domain.enums.VerificationStatus;
import jakarta.persistence.*;
import org.locationtech.jts.geom.Point;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "incidents")
public class Incident {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reporter_id", nullable = false)
    private User reporter;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "disaster_type", nullable = false)
    private DisasterType disasterType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private IncidentSeverity severity;

    @Column(columnDefinition = "geometry(Point, 4326)", nullable = false)
    private Point location;

    @Column(name = "reported_time", nullable = false)
    private Instant reportedTime = Instant.now();

    @Enumerated(EnumType.STRING)
    @Column(name = "verification_status", nullable = false)
    private VerificationStatus verificationStatus = VerificationStatus.PENDING;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "verifier_id")
    private User verifier;

    @Column(name = "verification_time")
    private Instant verificationTime;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public Incident() {
    }

    public Incident(User reporter, String title, String description, DisasterType disasterType,
                    IncidentSeverity severity, Point location) {
        this.reporter = reporter;
        this.title = title;
        this.description = description;
        this.disasterType = disasterType;
        this.severity = severity;
        this.location = location;
        this.reportedTime = Instant.now();
        this.verificationStatus = VerificationStatus.PENDING;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public User getReporter() {
        return reporter;
    }

    public void setReporter(User reporter) {
        this.reporter = reporter;
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

    public DisasterType getDisasterType() {
        return disasterType;
    }

    public void setDisasterType(DisasterType disasterType) {
        this.disasterType = disasterType;
    }

    public IncidentSeverity getSeverity() {
        return severity;
    }

    public void setSeverity(IncidentSeverity severity) {
        this.severity = severity;
    }

    public Point getLocation() {
        return location;
    }

    public void setLocation(Point location) {
        this.location = location;
    }

    public Instant getReportedTime() {
        return reportedTime;
    }

    public void setReportedTime(Instant reportedTime) {
        this.reportedTime = reportedTime;
    }

    public VerificationStatus getVerificationStatus() {
        return verificationStatus;
    }

    public void setVerificationStatus(VerificationStatus verificationStatus) {
        this.verificationStatus = verificationStatus;
    }

    public User getVerifier() {
        return verifier;
    }

    public void setVerifier(User verifier) {
        this.verifier = verifier;
    }

    public Instant getVerificationTime() {
        return verificationTime;
    }

    public void setVerificationTime(Instant verificationTime) {
        this.verificationTime = verificationTime;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
