package com.disaster.response.service;

import com.disaster.response.domain.entity.ResponderTeam;
import com.disaster.response.domain.entity.TeamCapability;
import com.disaster.response.domain.entity.TeamMember;
import com.disaster.response.domain.entity.User;
import com.disaster.response.domain.enums.AvailabilityStatus;
import com.disaster.response.domain.enums.CapabilityType;
import com.disaster.response.domain.enums.MembershipStatus;
import com.disaster.response.dto.team.*;
import com.disaster.response.exception.DuplicateResourceException;
import com.disaster.response.exception.ResourceNotFoundException;
import com.disaster.response.gis.GisUtil;
import com.disaster.response.repository.ResponderTeamRepository;
import com.disaster.response.repository.TeamCapabilityRepository;
import com.disaster.response.repository.TeamMemberRepository;
import com.disaster.response.repository.UserRepository;
import org.locationtech.jts.geom.Point;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class TeamService {

    private final ResponderTeamRepository teamRepository;
    private final TeamMemberRepository memberRepository;
    private final TeamCapabilityRepository capabilityRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;

    public TeamService(ResponderTeamRepository teamRepository,
                       TeamMemberRepository memberRepository,
                       TeamCapabilityRepository capabilityRepository,
                       UserRepository userRepository,
                       AuditService auditService) {
        this.teamRepository = teamRepository;
        this.memberRepository = memberRepository;
        this.capabilityRepository = capabilityRepository;
        this.userRepository = userRepository;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public Page<TeamResponse> getAllTeams(AvailabilityStatus status, Pageable pageable) {
        if (status != null) {
            return teamRepository.findByAvailabilityStatus(status, pageable).map(TeamResponse::from);
        }
        return teamRepository.findAll(pageable).map(TeamResponse::from);
    }

    @Transactional(readOnly = true)
    public TeamResponse getTeamById(UUID teamId) {
        ResponderTeam team = teamRepository.findById(teamId)
                .orElseThrow(() -> new ResourceNotFoundException("Responder team not found with ID: " + teamId));
        return TeamResponse.from(team);
    }

    @Transactional
    public TeamResponse createTeam(CreateTeamRequest request, User admin) {
        Point baseLoc = GisUtil.createPoint(request.baseLocation().latitude(), request.baseLocation().longitude());
        ResponderTeam team = new ResponderTeam(
                request.name(),
                request.teamType(),
                AvailabilityStatus.AVAILABLE,
                baseLoc,
                request.contactDetails()
        );

        ResponderTeam saved = teamRepository.save(team);

        if (request.capabilities() != null) {
            for (CapabilityType cap : request.capabilities()) {
                TeamCapability tc = new TeamCapability(saved, cap, null);
                capabilityRepository.save(tc);
                saved.getCapabilities().add(tc);
            }
        }

        auditService.record(admin, "CREATE_TEAM", "ResponderTeam", saved.getId().toString(), null, saved.getName());
        return TeamResponse.from(saved);
    }

    @Transactional
    public TeamResponse updateTeam(UUID teamId, UpdateTeamRequest request, User admin) {
        ResponderTeam team = teamRepository.findById(teamId)
                .orElseThrow(() -> new ResourceNotFoundException("Responder team not found with ID: " + teamId));

        String oldValues = "name=" + team.getName() + ", type=" + team.getTeamType();

        if (request.name() != null) team.setName(request.name());
        if (request.teamType() != null) team.setTeamType(request.teamType());
        if (request.contactDetails() != null) team.setContactDetails(request.contactDetails());
        if (request.baseLocation() != null) {
            team.setBaseLocation(GisUtil.createPoint(request.baseLocation().latitude(), request.baseLocation().longitude()));
        }

        ResponderTeam saved = teamRepository.save(team);
        String newValues = "name=" + saved.getName() + ", type=" + saved.getTeamType();
        auditService.record(admin, "UPDATE_TEAM", "ResponderTeam", saved.getId().toString(), oldValues, newValues);

        return TeamResponse.from(saved);
    }

    @Transactional
    public TeamResponse updateTeamStatus(UUID teamId, AvailabilityStatus newStatus, User actor) {
        ResponderTeam team = teamRepository.findById(teamId)
                .orElseThrow(() -> new ResourceNotFoundException("Responder team not found with ID: " + teamId));

        AvailabilityStatus oldStatus = team.getAvailabilityStatus();
        team.setAvailabilityStatus(newStatus);
        ResponderTeam saved = teamRepository.save(team);

        auditService.record(actor, "UPDATE_TEAM_STATUS", "ResponderTeam", saved.getId().toString(),
                "status=" + oldStatus, "status=" + newStatus);

        return TeamResponse.from(saved);
    }

    @Transactional
    public TeamResponse setTeamMembers(UUID teamId, List<UUID> userIds, User admin) {
        ResponderTeam team = teamRepository.findById(teamId)
                .orElseThrow(() -> new ResourceNotFoundException("Responder team not found with ID: " + teamId));

        // Clear existing memberships
        List<TeamMember> currentMembers = memberRepository.findByTeamIdAndMembershipStatus(teamId, MembershipStatus.ACTIVE);
        for (TeamMember m : currentMembers) {
            m.setMembershipStatus(MembershipStatus.INACTIVE);
            memberRepository.save(m);
        }

        // Add new members
        List<TeamMember> newMembers = new ArrayList<>();
        for (UUID userId : userIds) {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));

            TeamMember member = memberRepository.findByTeamIdAndUserId(teamId, userId)
                    .orElse(new TeamMember(team, user, MembershipStatus.ACTIVE));
            member.setMembershipStatus(MembershipStatus.ACTIVE);
            newMembers.add(memberRepository.save(member));
        }

        team.setMembers(newMembers);
        auditService.record(admin, "SET_TEAM_MEMBERS", "ResponderTeam", teamId.toString(),
                "count=" + currentMembers.size(), "count=" + newMembers.size());

        return TeamResponse.from(team);
    }

    @Transactional(readOnly = true)
    public List<UUID> getTeamIdsForUser(UUID userId) {
        return memberRepository.findByUserIdAndMembershipStatus(userId, MembershipStatus.ACTIVE)
                .stream()
                .map(m -> m.getTeam().getId())
                .toList();
    }
}
