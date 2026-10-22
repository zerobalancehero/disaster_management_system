package com.disaster.response.repository;

import com.disaster.response.domain.entity.IncidentAssignment;
import com.disaster.response.domain.enums.AssignmentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface IncidentAssignmentRepository extends JpaRepository<IncidentAssignment, UUID> {

    List<IncidentAssignment> findByTeamIdOrderByAssignmentTimeDesc(UUID teamId);

    Page<IncidentAssignment> findByTeamIdInOrderByAssignmentTimeDesc(Collection<UUID> teamIds, Pageable pageable);

    Optional<IncidentAssignment> findByIdAndTeamIdIn(UUID id, Collection<UUID> teamIds);

    List<IncidentAssignment> findByIncidentId(UUID incidentId);

    Optional<IncidentAssignment> findFirstByIncidentIdAndAssignmentStatusNotIn(UUID incidentId, Collection<AssignmentStatus> terminalStatuses);

    boolean existsByIncidentIdAndAssignmentStatusNotIn(UUID incidentId, Collection<AssignmentStatus> terminalStatuses);

    long countByAssignmentStatus(AssignmentStatus status);

    long countByTeamIdAndAssignmentStatus(UUID teamId, AssignmentStatus status);

    @Query("SELECT a FROM IncidentAssignment a WHERE a.assignmentStatus NOT IN ('RESOLVED', 'CANCELLED', 'REJECTED')")
    List<IncidentAssignment> findActiveAssignments();
}
