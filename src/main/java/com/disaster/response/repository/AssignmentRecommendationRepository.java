package com.disaster.response.repository;

import com.disaster.response.domain.entity.AssignmentRecommendation;
import com.disaster.response.domain.enums.RecommendationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AssignmentRecommendationRepository extends JpaRepository<AssignmentRecommendation, UUID> {
    List<AssignmentRecommendation> findByIncidentIdOrderByRankAsc(UUID incidentId);
    List<AssignmentRecommendation> findByIncidentIdAndRecommendationStatus(UUID incidentId, RecommendationStatus status);
}
