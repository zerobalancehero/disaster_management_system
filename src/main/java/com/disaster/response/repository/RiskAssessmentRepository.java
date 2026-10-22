package com.disaster.response.repository;

import com.disaster.response.domain.entity.RiskAssessment;
import com.disaster.response.domain.enums.DisasterType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RiskAssessmentRepository extends JpaRepository<RiskAssessment, UUID> {

    List<RiskAssessment> findByRegionIdOrderByPredictionTimestampDesc(UUID regionId);

    Page<RiskAssessment> findByRegionIdOrderByPredictionTimestampDesc(UUID regionId, Pageable pageable);

    Optional<RiskAssessment> findFirstByRegionIdAndDisasterTypeOrderByPredictionTimestampDesc(UUID regionId, DisasterType disasterType);

    @Query("SELECT r FROM RiskAssessment r WHERE r.predictionTimestamp = (SELECT MAX(r2.predictionTimestamp) FROM RiskAssessment r2 WHERE r2.region.id = r.region.id AND r2.disasterType = r.disasterType)")
    List<RiskAssessment> findLatestRiskAssessmentsForAllRegions();
}
