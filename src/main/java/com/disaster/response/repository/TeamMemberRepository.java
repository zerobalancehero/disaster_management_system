package com.disaster.response.repository;

import com.disaster.response.domain.entity.TeamMember;
import com.disaster.response.domain.enums.MembershipStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TeamMemberRepository extends JpaRepository<TeamMember, UUID> {
    List<TeamMember> findByTeamIdAndMembershipStatus(UUID teamId, MembershipStatus status);
    List<TeamMember> findByUserIdAndMembershipStatus(UUID userId, MembershipStatus status);
    Optional<TeamMember> findByTeamIdAndUserId(UUID teamId, UUID userId);
    boolean existsByTeamIdAndUserIdAndMembershipStatus(UUID teamId, UUID userId, MembershipStatus status);
}
