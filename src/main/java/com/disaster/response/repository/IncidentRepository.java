package com.disaster.response.repository;

import com.disaster.response.domain.entity.Incident;
import com.disaster.response.domain.enums.IncidentSeverity;
import com.disaster.response.domain.enums.VerificationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface IncidentRepository extends JpaRepository<Incident, UUID> {

    Page<Incident> findByReporterIdOrderByReportedTimeDesc(UUID reporterId, Pageable pageable);

    Optional<Incident> findByIdAndReporterId(UUID id, UUID reporterId);

    Page<Incident> findByVerificationStatusOrderByReportedTimeDesc(VerificationStatus status, Pageable pageable);

    List<Incident> findByVerificationStatusOrderByReportedTimeDesc(VerificationStatus status);

    long countByVerificationStatus(VerificationStatus status);

    long countBySeverity(IncidentSeverity severity);
}
