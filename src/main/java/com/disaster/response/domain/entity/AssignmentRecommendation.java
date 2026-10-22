package com.disaster.response.domain.entity;

import com.disaster.response.domain.enums.RecommendationStatus;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "assignment_recommendations")
public class AssignmentRecommendation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "incident_id", nullable = false)
    private Incident incident;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id", nullable = false)
    private ResponderTeam team;

    @Column(nullable = false)
    private int rank;

    @Column(name = "estimated_travel_time_minutes")
    private Integer estimatedTravelTimeMinutes;

    @Column(nullable = false)
    private boolean eligibility = true;

    @Column(name = "availability_snapshot", nullable = false)
    private String availabilitySnapshot;

    @Column(name = "recommendation_reason", columnDefinition = "TEXT", nullable = false)
    private String recommendationReason;

    @Column(name = "generation_time", nullable = false)
    private Instant generationTime = Instant.now();

    @Enumerated(EnumType.STRING)
    @Column(name = "recommendation_status", nullable = false)
    private RecommendationStatus recommendationStatus = RecommendationStatus.PROPOSED;

    public AssignmentRecommendation() {
    }

    public AssignmentRecommendation(Incident incident, ResponderTeam team, int rank,
                                    Integer estimatedTravelTimeMinutes, boolean eligibility,
                                    String availabilitySnapshot, String recommendationReason) {
        this.incident = incident;
        this.team = team;
        this.rank = rank;
        this.estimatedTravelTimeMinutes = estimatedTravelTimeMinutes;
        this.eligibility = eligibility;
        this.availabilitySnapshot = availabilitySnapshot;
        this.recommendationReason = recommendationReason;
        this.generationTime = Instant.now();
        this.recommendationStatus = RecommendationStatus.PROPOSED;
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

    public int getRank() {
        return rank;
    }

    public void setRank(int rank) {
        this.rank = rank;
    }

    public Integer getEstimatedTravelTimeMinutes() {
        return estimatedTravelTimeMinutes;
    }

    public void setEstimatedTravelTimeMinutes(Integer estimatedTravelTimeMinutes) {
        this.estimatedTravelTimeMinutes = estimatedTravelTimeMinutes;
    }

    public boolean isEligibility() {
        return eligibility;
    }

    public void setEligibility(boolean eligibility) {
        this.eligibility = eligibility;
    }

    public String getAvailabilitySnapshot() {
        return availabilitySnapshot;
    }

    public void setAvailabilitySnapshot(String availabilitySnapshot) {
        this.availabilitySnapshot = availabilitySnapshot;
    }

    public String getRecommendationReason() {
        return recommendationReason;
    }

    public void setRecommendationReason(String recommendationReason) {
        this.recommendationReason = recommendationReason;
    }

    public Instant getGenerationTime() {
        return generationTime;
    }

    public void setGenerationTime(Instant generationTime) {
        this.generationTime = generationTime;
    }

    public RecommendationStatus getRecommendationStatus() {
        return recommendationStatus;
    }

    public void setRecommendationStatus(RecommendationStatus recommendationStatus) {
        this.recommendationStatus = recommendationStatus;
    }
}
