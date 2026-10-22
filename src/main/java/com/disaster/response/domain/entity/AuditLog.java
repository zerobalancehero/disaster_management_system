package com.disaster.response.domain.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "audit_logs")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_id")
    private User actor;

    @Column(nullable = false)
    private String action;

    @Column(name = "entity_type", nullable = false)
    private String entityType;

    @Column(name = "entity_id", nullable = false)
    private String entityId;

    @Column(name = "old_values", columnDefinition = "TEXT")
    private String oldValues;

    @Column(name = "new_values", columnDefinition = "TEXT")
    private String newValues;

    @Column(nullable = false, updatable = false)
    private Instant timestamp = Instant.now();

    public AuditLog() {
    }

    public AuditLog(User actor, String action, String entityType, String entityId, String oldValues, String newValues) {
        this.actor = actor;
        this.action = action;
        this.entityType = entityType;
        this.entityId = entityId;
        this.oldValues = oldValues;
        this.newValues = newValues;
        this.timestamp = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public User getActor() {
        return actor;
    }

    public void setActor(User actor) {
        this.actor = actor;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getEntityType() {
        return entityType;
    }

    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }

    public String getEntityId() {
        return entityId;
    }

    public void setEntityId(String entityId) {
        this.entityId = entityId;
    }

    public String getOldValues() {
        return oldValues;
    }

    public void setOldValues(String oldValues) {
        this.oldValues = oldValues;
    }

    public String getNewValues() {
        return newValues;
    }

    public void setNewValues(String newValues) {
        this.newValues = newValues;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }
}
