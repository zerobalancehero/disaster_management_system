package com.disaster.response.repository;

import com.disaster.response.domain.entity.TeamCapability;
import com.disaster.response.domain.enums.CapabilityType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TeamCapabilityRepository extends JpaRepository<TeamCapability, UUID> {
    List<TeamCapability> findByTeamId(UUID teamId);
    List<TeamCapability> findByCapability(CapabilityType capability);
}
