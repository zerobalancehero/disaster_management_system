package com.disaster.response.domain.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "model_registry", uniqueConstraints = {
    @UniqueConstraint(name = "uk_model_name_version", columnNames = {"model_name", "version"})
})
public class ModelRegistry {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "model_name", nullable = false)
    private String modelName;

    @Column(nullable = false)
    private String version;

    @Column(name = "artifact_location", nullable = false)
    private String artifactLocation;

    @Column(nullable = false)
    private String framework;

    @Column(nullable = false)
    private boolean active = false;

    @Column(name = "feature_schema_version", nullable = false)
    private String featureSchemaVersion;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "evaluation_metadata", columnDefinition = "TEXT")
    private String evaluationMetadata;

    public ModelRegistry() {
    }

    public ModelRegistry(String modelName, String version, String artifactLocation,
                         String framework, boolean active, String featureSchemaVersion,
                         String evaluationMetadata) {
        this.modelName = modelName;
        this.version = version;
        this.artifactLocation = artifactLocation;
        this.framework = framework;
        this.active = active;
        this.featureSchemaVersion = featureSchemaVersion;
        this.evaluationMetadata = evaluationMetadata;
        this.createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getModelName() {
        return modelName;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getArtifactLocation() {
        return artifactLocation;
    }

    public void setArtifactLocation(String artifactLocation) {
        this.artifactLocation = artifactLocation;
    }

    public String getFramework() {
        return framework;
    }

    public void setFramework(String framework) {
        this.framework = framework;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public String getFeatureSchemaVersion() {
        return featureSchemaVersion;
    }

    public void setFeatureSchemaVersion(String featureSchemaVersion) {
        this.featureSchemaVersion = featureSchemaVersion;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public String getEvaluationMetadata() {
        return evaluationMetadata;
    }

    public void setEvaluationMetadata(String evaluationMetadata) {
        this.evaluationMetadata = evaluationMetadata;
    }
}
