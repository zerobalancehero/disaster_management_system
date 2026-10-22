package com.disaster.response.repository;

import com.disaster.response.domain.entity.SafetyInstruction;
import com.disaster.response.domain.enums.AlertSeverity;
import com.disaster.response.domain.enums.DisasterType;
import com.disaster.response.domain.enums.TargetAudience;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SafetyInstructionRepository extends JpaRepository<SafetyInstruction, UUID> {

    List<SafetyInstruction> findByDisasterTypeAndActiveTrue(DisasterType disasterType);

    @Query("SELECT s FROM SafetyInstruction s WHERE s.active = true AND s.disasterType = :disasterType AND s.language = :lang ORDER BY s.title ASC")
    List<SafetyInstruction> findApplicableInstructions(
            @Param("disasterType") DisasterType disasterType,
            @Param("lang") String lang);

    Page<SafetyInstruction> findByActive(boolean active, Pageable pageable);
}
