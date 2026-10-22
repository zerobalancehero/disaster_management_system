package com.disaster.response.domain.entity;

import com.disaster.response.domain.enums.AlertSeverity;
import com.disaster.response.domain.enums.DisasterType;
import com.disaster.response.domain.enums.TargetAudience;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "safety_instructions")
public class SafetyInstruction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(name = "disaster_type", nullable = false)
    private DisasterType disasterType;

    @Enumerated(EnumType.STRING)
    @Column(name = "minimum_severity", nullable = false)
    private AlertSeverity minimumSeverity = AlertSeverity.LOW;

    @Column(name = "instruction_text", columnDefinition = "TEXT", nullable = false)
    private String instructionText;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_audience", nullable = false)
    private TargetAudience targetAudience = TargetAudience.GENERAL_PUBLIC;

    @Column(length = 10, nullable = false)
    private String language = "en";

    @Column(nullable = false)
    private boolean active = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creator_id")
    private User creator;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updater_id")
    private User updater;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public SafetyInstruction() {
    }

    public SafetyInstruction(String title, DisasterType disasterType, AlertSeverity minimumSeverity,
                             String instructionText, TargetAudience targetAudience, String language,
                             User creator) {
        this.title = title;
        this.disasterType = disasterType;
        this.minimumSeverity = minimumSeverity != null ? minimumSeverity : AlertSeverity.LOW;
        this.instructionText = instructionText;
        this.targetAudience = targetAudience != null ? targetAudience : TargetAudience.GENERAL_PUBLIC;
        this.language = language != null ? language : "en";
        this.active = true;
        this.creator = creator;
        this.updater = creator;
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

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public DisasterType getDisasterType() {
        return disasterType;
    }

    public void setDisasterType(DisasterType disasterType) {
        this.disasterType = disasterType;
    }

    public AlertSeverity getMinimumSeverity() {
        return minimumSeverity;
    }

    public void setMinimumSeverity(AlertSeverity minimumSeverity) {
        this.minimumSeverity = minimumSeverity;
    }

    public String getInstructionText() {
        return instructionText;
    }

    public void setInstructionText(String instructionText) {
        this.instructionText = instructionText;
    }

    public TargetAudience getTargetAudience() {
        return targetAudience;
    }

    public void setTargetAudience(TargetAudience targetAudience) {
        this.targetAudience = targetAudience;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public User getCreator() {
        return creator;
    }

    public void setCreator(User creator) {
        this.creator = creator;
    }

    public User getUpdater() {
        return updater;
    }

    public void setUpdater(User updater) {
        this.updater = updater;
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
