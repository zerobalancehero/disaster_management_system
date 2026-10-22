package com.disaster.response.service;

import com.disaster.response.domain.entity.*;
import com.disaster.response.domain.enums.AssignmentStatus;
import com.disaster.response.domain.enums.AvailabilityStatus;
import com.disaster.response.domain.enums.NotificationType;
import com.disaster.response.domain.enums.RecommendationStatus;
import com.disaster.response.dto.assignment.AssignmentResponse;
import com.disaster.response.dto.assignment.ResponseUpdateRequest;
import com.disaster.response.dto.assignment.UpdateAssignmentStatusRequest;
import com.disaster.response.exception.IdempotencyConflictException;
import com.disaster.response.exception.InvalidStateTransitionException;
import com.disaster.response.exception.ResourceNotFoundException;
import com.disaster.response.exception.UnauthorizedActionException;
import com.disaster.response.gis.GisUtil;
import com.disaster.response.repository.*;
import org.locationtech.jts.geom.Point;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class AssignmentService {

    private static final Logger log = LoggerFactory.getLogger(AssignmentService.class);

    private static final List<AssignmentStatus> TERMINAL_STATUSES =
            List.of(AssignmentStatus.RESOLVED, AssignmentStatus.CANCELLED, AssignmentStatus.REJECTED);

    private final IncidentAssignmentRepository assignmentRepository;
    private final AssignmentRecommendationRepository recommendationRepository;
    private final IncidentRepository incidentRepository;
    private final ResponderTeamRepository teamRepository;
    private final ResponseUpdateRepository responseUpdateRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final NotificationService notificationService;
    private final AuditService auditService;
    private final TeamService teamService;

    public AssignmentService(
            IncidentAssignmentRepository assignmentRepository,
            AssignmentRecommendationRepository recommendationRepository,
            IncidentRepository incidentRepository,
            ResponderTeamRepository teamRepository,
            ResponseUpdateRepository responseUpdateRepository,
            TeamMemberRepository teamMemberRepository,
            NotificationService notificationService,
            AuditService auditService,
            TeamService teamService) {
        this.assignmentRepository = assignmentRepository;
        this.recommendationRepository = recommendationRepository;
        this.incidentRepository = incidentRepository;
        this.teamRepository = teamRepository;
        this.responseUpdateRepository = responseUpdateRepository;
        this.teamMemberRepository = teamMemberRepository;
        this.notificationService = notificationService;
        this.auditService = auditService;
        this.teamService = teamService;
    }

    /**
     * Approves a recommendation and dispatches the team.
     * Enforces:
     * - Rule 5: Explicit admin approval required before dispatch.
     * - Rule 6: Rejected recommendations cannot dispatch.
     * - Rule 11: Idempotency and double-approval prevention.
     */
    @Transactional
    public AssignmentResponse approveRecommendationAndDispatch(UUID recommendationId, User admin) {
        AssignmentRecommendation recommendation = recommendationRepository.findById(recommendationId)
                .orElseThrow(() -> new ResourceNotFoundException("Recommendation not found with ID: " + recommendationId));

        if (recommendation.getRecommendationStatus() != RecommendationStatus.PROPOSED) {
            throw new IdempotencyConflictException(
                    "Recommendation cannot be approved. Current status is " + recommendation.getRecommendationStatus()
            );
        }

        Incident incident = recommendation.getIncident();
        ResponderTeam team = recommendation.getTeam();

        // Ensure no other active assignment exists for this incident
        if (assignmentRepository.existsByIncidentIdAndAssignmentStatusNotIn(incident.getId(), TERMINAL_STATUSES)) {
            throw new IdempotencyConflictException(
                    "An active assignment already exists for this incident. Cannot create duplicate assignment."
            );
        }

        // 1. Mark this recommendation as APPROVED
        recommendation.setRecommendationStatus(RecommendationStatus.APPROVED);
        recommendationRepository.save(recommendation);

        // 2. Mark any other proposed recommendations for this incident as SUPERSEDED
        List<AssignmentRecommendation> siblings = recommendationRepository
                .findByIncidentIdAndRecommendationStatus(incident.getId(), RecommendationStatus.PROPOSED);
        for (AssignmentRecommendation sib : siblings) {
            sib.setRecommendationStatus(RecommendationStatus.SUPERSEDED);
            recommendationRepository.save(sib);
        }

        // 3. Update team availability to DISPATCHED
        team.setAvailabilityStatus(AvailabilityStatus.DISPATCHED);
        teamRepository.save(team);

        // 4. Create confirmed IncidentAssignment with APPROVED status
        IncidentAssignment assignment = new IncidentAssignment(
                incident,
                team,
                admin,
                AssignmentStatus.APPROVED
        );
        IncidentAssignment savedAssignment = assignmentRepository.save(assignment);

        // 5. Audit record
        auditService.record(admin, "APPROVE_DISPATCH", "IncidentAssignment",
                savedAssignment.getId().toString(),
                "recommendation=" + recommendationId,
                "team=" + team.getName() + ", status=APPROVED");

        // 6. Send in-app notification to all active team members
        List<TeamMember> members = teamMemberRepository.findByTeamIdAndMembershipStatus(
                team.getId(), com.disaster.response.domain.enums.MembershipStatus.ACTIVE);
        for (TeamMember member : members) {
            notificationService.sendInAppNotification(
                    member.getUser(),
                    NotificationType.DISPATCH_ALERT,
                    "New Incident Assignment: " + incident.getTitle(),
                    "Your team '" + team.getName() + "' has been assigned to incident '" + incident.getTitle() + "'. Please review and accept.",
                    "IncidentAssignment",
                    savedAssignment.getId().toString()
            );
        }

        log.info("Admin {} approved recommendation {} and dispatched team {} for incident {}",
                admin.getEmail(), recommendationId, team.getName(), incident.getId());

        return AssignmentResponse.from(savedAssignment);
    }

    /**
     * Reassigns an incident by cancelling the existing active assignment.
     */
    @Transactional
    public AssignmentResponse reassignIncident(UUID incidentId, String reason, User admin) {
        IncidentAssignment currentAssignment = assignmentRepository
                .findFirstByIncidentIdAndAssignmentStatusNotIn(incidentId, TERMINAL_STATUSES)
                .orElseThrow(() -> new ResourceNotFoundException("No active assignment found for incident: " + incidentId));

        AssignmentStatus oldStatus = currentAssignment.getAssignmentStatus();
        currentAssignment.setAssignmentStatus(AssignmentStatus.CANCELLED);
        currentAssignment.setRejectionOrCancellationReason("Reassigned by admin: " + reason);
        currentAssignment.setCompletionTime(Instant.now());
        IncidentAssignment savedAssignment = assignmentRepository.save(currentAssignment);

        // Free the team back to AVAILABLE
        ResponderTeam team = currentAssignment.getTeam();
        team.setAvailabilityStatus(AvailabilityStatus.AVAILABLE);
        teamRepository.save(team);

        auditService.record(admin, "REASSIGN_INCIDENT", "IncidentAssignment",
                savedAssignment.getId().toString(),
                "status=" + oldStatus,
                "status=CANCELLED, reason=" + reason);

        // Notify previous team members
        List<TeamMember> members = teamMemberRepository.findByTeamIdAndMembershipStatus(
                team.getId(), com.disaster.response.domain.enums.MembershipStatus.ACTIVE);
        for (TeamMember member : members) {
            notificationService.sendInAppNotification(
                    member.getUser(),
                    NotificationType.STATUS_CHANGE,
                    "Assignment Cancelled / Reassigned",
                    "Your assignment for incident '" + currentAssignment.getIncident().getTitle() + "' was cancelled for reassignment.",
                    "IncidentAssignment",
                    savedAssignment.getId().toString()
            );
        }

        return AssignmentResponse.from(savedAssignment);
    }

    /**
     * Retrieves assignments for the current authenticated responder.
     * Enforces Rule 7: Responders can only access assignments for authorized teams.
     */
    @Transactional(readOnly = true)
    public Page<AssignmentResponse> getResponderAssignments(User responder, Pageable pageable) {
        List<UUID> teamIds = teamService.getTeamIdsForUser(responder.getId());
        if (teamIds.isEmpty()) {
            return Page.empty(pageable);
        }
        return assignmentRepository.findByTeamIdInOrderByAssignmentTimeDesc(teamIds, pageable)
                .map(AssignmentResponse::from);
    }

    /**
     * Retrieves specific assignment ensuring responder belongs to assigned team.
     * Enforces Rule 7.
     */
    @Transactional(readOnly = true)
    public AssignmentResponse getResponderAssignmentById(UUID assignmentId, User responder) {
        List<UUID> teamIds = teamService.getTeamIdsForUser(responder.getId());
        IncidentAssignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Assignment not found with ID: " + assignmentId));

        if (!teamIds.contains(assignment.getTeam().getId())) {
            throw new UnauthorizedActionException("You are not an authorized member of the team assigned to this incident.");
        }

        return AssignmentResponse.from(assignment);
    }

    /**
     * Responders transition assignment status following explicit state machine.
     * Enforces Rule 9: Assignment status changes must follow explicit state machine.
     */
    @Transactional
    public AssignmentResponse updateAssignmentStatus(UUID assignmentId, UpdateAssignmentStatusRequest request, User responder) {
        List<UUID> teamIds = teamService.getTeamIdsForUser(responder.getId());
        IncidentAssignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Assignment not found with ID: " + assignmentId));

        if (!teamIds.contains(assignment.getTeam().getId())) {
            throw new UnauthorizedActionException("You are not an authorized member of the assigned team.");
        }

        AssignmentStatus currentStatus = assignment.getAssignmentStatus();
        AssignmentStatus targetStatus = request.status();

        if (!currentStatus.canTransitionTo(targetStatus)) {
            throw new InvalidStateTransitionException(
                    String.format("Invalid assignment status transition from %s to %s.", currentStatus, targetStatus)
            );
        }

        assignment.setAssignmentStatus(targetStatus);
        if (targetStatus == AssignmentStatus.RESOLVED || targetStatus == AssignmentStatus.CANCELLED) {
            assignment.setCompletionTime(Instant.now());
            // Return team to AVAILABLE
            ResponderTeam team = assignment.getTeam();
            team.setAvailabilityStatus(AvailabilityStatus.AVAILABLE);
            teamRepository.save(team);
        } else if (targetStatus == AssignmentStatus.ON_SCENE || targetStatus == AssignmentStatus.EN_ROUTE) {
            ResponderTeam team = assignment.getTeam();
            team.setAvailabilityStatus(AvailabilityStatus.DEPLOYED);
            teamRepository.save(team);
        }

        Point updateLoc = request.location() != null
                ? GisUtil.createPoint(request.location().latitude(), request.location().longitude())
                : null;

        String msg = (request.message() != null && !request.message().isBlank())
                ? request.message()
                : "Status changed to " + targetStatus;

        ResponseUpdate update = new ResponseUpdate(assignment, responder, targetStatus.name(), msg, updateLoc);
        responseUpdateRepository.save(update);
        assignment.getResponseUpdates().add(update);

        IncidentAssignment saved = assignmentRepository.save(assignment);
        auditService.record(responder, "UPDATE_ASSIGNMENT_STATUS", "IncidentAssignment",
                saved.getId().toString(), "status=" + currentStatus, "status=" + targetStatus);

        log.info("Responder {} updated assignment {} to {}", responder.getEmail(), assignmentId, targetStatus);
        return AssignmentResponse.from(saved);
    }

    /**
     * Responders submit progress updates.
     */
    @Transactional
    public AssignmentResponse addResponseUpdate(UUID assignmentId, ResponseUpdateRequest request, User responder) {
        List<UUID> teamIds = teamService.getTeamIdsForUser(responder.getId());
        IncidentAssignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Assignment not found with ID: " + assignmentId));

        if (!teamIds.contains(assignment.getTeam().getId())) {
            throw new UnauthorizedActionException("You are not authorized to post updates for this team's assignment.");
        }

        Point loc = request.location() != null
                ? GisUtil.createPoint(request.location().latitude(), request.location().longitude())
                : null;

        ResponseUpdate update = new ResponseUpdate(assignment, responder, request.status(), request.message(), loc);
        responseUpdateRepository.save(update);
        assignment.getResponseUpdates().add(update);

        return AssignmentResponse.from(assignment);
    }
}
