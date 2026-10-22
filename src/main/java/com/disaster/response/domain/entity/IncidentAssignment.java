package com.disaster.response.domain.entity;

import com.disaster.response.domain.enums.AssignmentStatus;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "incident_assignments")
public class IncidentAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "incident_id", nullable = false)
    private Incident incident;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id", nullable = false)
    private ResponderTeam team;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approving_admin_id", nullable = false)
    private User approvingAdmin;

    @Column(name = "approval_time", nullable = false)
    private Instant approvalTime = Instant.now();

    @Enumerated(EnumType.STRING)
    @Column(name = "assignment_status", nullable = false)
    private AssignmentStatus assignmentStatus = AssignmentStatus.PENDING_APPROVAL;

    @Column(name = "assignment_time", nullable = false)
    private Instant assignmentTime = Instant.now();

    @Column(name = "completion_time")
    private Instant completionTime;

    @Column(name = "rejection_or_cancellation_reason", columnDefinition = "TEXT")
    private String rejectionOrCancellationReason;

    @Version
    @Column(nullable = false)
    private Long version = 0L;

    @OneToMany(mappedBy = "assignment", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ResponseUpdate> responseUpdates = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public IncidentAssignment() {
    }

    public IncidentAssignment(Incident incident, ResponderTeam team, User approvingAdmin, AssignmentStatus status) {
        this.incident = incident;
        this.team = team;
        this.approvingAdmin = approvingAdmin;
        this.assignmentStatus = status != null ? status : AssignmentStatus.PENDING_APPROVAL;
        this.approvalTime = Instant.now();
        this.assignmentTime = Instant.now();
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

    public Incident getIncident() {
        return incident;
    }

    public void setIncident(Incident incident) {
        this.incident = incident;
    }

    public ResponderTeam getTeam() {
        return team;
    }

    public void setTeam(ResponderTeam team) {
        this.team = team;
    }

    public User getApprovingAdmin() {
        return approvingAdmin;
    }

    public void setApprovingAdmin(User approvingAdmin) {
        this.approvingAdmin = approvingAdmin;
    }

    public Instant getApprovalTime() {
        return approvalTime;
    }

    public void setApprovalTime(Instant approvalTime) {
        this.approvalTime = approvalTime;
    }

    public AssignmentStatus getAssignmentStatus() {
        return assignmentStatus;
    }

    public void setAssignmentStatus(AssignmentStatus assignmentStatus) {
        this.assignmentStatus = assignmentStatus;
    }

    public Instant getAssignmentTime() {
        return assignmentTime;
    }

    public void setAssignmentTime(Instant assignmentTime) {
        this.assignmentTime = assignmentTime;
    }

    public Instant getCompletionTime() {
        return completionTime;
    }

    public void setCompletionTime(Instant completionTime) {
        this.completionTime = completionTime;
    }

    public String getRejectionOrCancellationReason() {
        return rejectionOrCancellationReason;
    }

    public void setRejectionOrCancellationReason(String rejectionOrCancellationReason) {
        this.rejectionOrCancellationReason = rejectionOrCancellationReason;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public List<ResponseUpdate> getResponseUpdates() {
        return responseUpdates;
    }

    public void setResponseUpdates(List<ResponseUpdate> responseUpdates) {
        this.responseUpdates = responseUpdates;
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
