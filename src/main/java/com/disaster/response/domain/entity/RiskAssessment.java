package com.disaster.response.domain.entity;

import com.disaster.response.domain.enums.DisasterType;
import com.disaster.response.domain.enums.PredictionHorizon;
import com.disaster.response.domain.enums.RiskCategory;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "risk_assessments")
public class RiskAssessment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "region_id", nullable = false)
    private Region region;

    @Enumerated(EnumType.STRING)
    @Column(name = "disaster_type", nullable = false)
    private DisasterType disasterType;

    @Column(nullable = false)
    private Double probability;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_category", nullable = false)
    private RiskCategory riskCategory;

    @Enumerated(EnumType.STRING)
    @Column(name = "prediction_horizon", nullable = false)
    private PredictionHorizon predictionHorizon;

    @Column(name = "prediction_timestamp", nullable = false)
    private Instant predictionTimestamp = Instant.now();

    @Column(name = "model_version", nullable = false)
    private String modelVersion;

    @Column(name = "feature_snapshot", columnDefinition = "TEXT")
    private String featureSnapshot;

    @Column(name = "inference_metadata", columnDefinition = "TEXT")
    private String inferenceMetadata;

    public RiskAssessment() {
    }

    public RiskAssessment(Region region, DisasterType disasterType, Double probability,
                          RiskCategory riskCategory, PredictionHorizon predictionHorizon,
                          String modelVersion, String featureSnapshot, String inferenceMetadata) {
        this.region = region;
        this.disasterType = disasterType;
        this.probability = probability;
        this.riskCategory = riskCategory;
        this.predictionHorizon = predictionHorizon;
        this.modelVersion = modelVersion;
        this.featureSnapshot = featureSnapshot;
        this.inferenceMetadata = inferenceMetadata;
        this.predictionTimestamp = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Region getRegion() {
        return region;
    }

    public void setRegion(Region region) {
        this.region = region;
    }

    public DisasterType getDisasterType() {
        return disasterType;
    }

    public void setDisasterType(DisasterType disasterType) {
        this.disasterType = disasterType;
    }

    public Double getProbability() {
        return probability;
    }

    public void setProbability(Double probability) {
        this.probability = probability;
    }

    public RiskCategory getRiskCategory() {
        return riskCategory;
    }

    public void setRiskCategory(RiskCategory riskCategory) {
        this.riskCategory = riskCategory;
    }

    public PredictionHorizon getPredictionHorizon() {
        return predictionHorizon;
    }

    public void setPredictionHorizon(PredictionHorizon predictionHorizon) {
        this.predictionHorizon = predictionHorizon;
    }

    public Instant getPredictionTimestamp() {
        return predictionTimestamp;
    }

    public void setPredictionTimestamp(Instant predictionTimestamp) {
        this.predictionTimestamp = predictionTimestamp;
    }

    public String getModelVersion() {
        return modelVersion;
    }

    public void setModelVersion(String modelVersion) {
        this.modelVersion = modelVersion;
    }

    public String getFeatureSnapshot() {
        return featureSnapshot;
    }

    public void setFeatureSnapshot(String featureSnapshot) {
        this.featureSnapshot = featureSnapshot;
    }

    public String getInferenceMetadata() {
        return inferenceMetadata;
    }

    public void setInferenceMetadata(String inferenceMetadata) {
        this.inferenceMetadata = inferenceMetadata;
    }
}
