package com.disaster.response.repository;

import com.disaster.response.domain.entity.ResponderTeam;
import com.disaster.response.domain.enums.AvailabilityStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ResponderTeamRepository extends JpaRepository<ResponderTeam, UUID> {
    List<ResponderTeam> findByAvailabilityStatus(AvailabilityStatus status);
    Page<ResponderTeam> findByAvailabilityStatus(AvailabilityStatus status, Pageable pageable);
}
