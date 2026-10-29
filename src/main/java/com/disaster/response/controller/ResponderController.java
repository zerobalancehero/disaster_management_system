package com.disaster.response.controller;

import com.disaster.response.config.UserPrincipal;
import com.disaster.response.domain.entity.User;
import com.disaster.response.domain.enums.AvailabilityStatus;
import com.disaster.response.dto.assignment.AssignmentResponse;
import com.disaster.response.dto.assignment.ResponseUpdateRequest;
import com.disaster.response.dto.assignment.UpdateAssignmentStatusRequest;
import com.disaster.response.dto.common.PagedResponse;
import com.disaster.response.dto.notification.NotificationResponse;
import com.disaster.response.dto.team.TeamResponse;
import com.disaster.response.dto.team.UpdateTeamStatusRequest;
import com.disaster.response.exception.ResourceNotFoundException;
import com.disaster.response.exception.UnauthorizedActionException;
import com.disaster.response.repository.UserRepository;
import com.disaster.response.service.AssignmentService;
import com.disaster.response.service.NotificationService;
import com.disaster.response.service.TeamService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/responder")
@Tag(name = "Responder Operations", description = "Responder assignments, status updates, team availability, and notifications")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasAnyRole('RESPONDER', 'ADMIN')")
public class ResponderController {

    private final AssignmentService assignmentService;
    private final TeamService teamService;
    private final NotificationService notificationService;
    private final UserRepository userRepository;

    public ResponderController(
            AssignmentService assignmentService,
            TeamService teamService,
            NotificationService notificationService,
            UserRepository userRepository) {
        this.assignmentService = assignmentService;
        this.teamService = teamService;
        this.notificationService = notificationService;
        this.userRepository = userRepository;
    }

    @GetMapping("/my-assignments")
    @Operation(summary = "View active and past assignments belonging strictly to the responder's authorized team(s)")
    public ResponseEntity<PagedResponse<AssignmentResponse>> getMyAssignments(
            @AuthenticationPrincipal UserPrincipal principal,
            @PageableDefault(size = 20, sort = "assignmentTime", direction = Sort.Direction.DESC) Pageable pageable) {
        User user = getAuthenticatedUser(principal);
        return ResponseEntity.ok(PagedResponse.from(assignmentService.getResponderAssignments(user, pageable)));
    }

    @GetMapping("/my-assignments/{id}")
    @Operation(summary = "View assignment details for an authorized team assignment")
    public ResponseEntity<AssignmentResponse> getMyAssignmentById(
            @PathVariable("id") UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {
        User user = getAuthenticatedUser(principal);
        return ResponseEntity.ok(assignmentService.getResponderAssignmentById(id, user));
    }

    @PatchMapping("/my-assignments/{id}/status")
    @Operation(summary = "Transition assignment status according to explicit state machine rules")
    public ResponseEntity<AssignmentResponse> updateAssignmentStatus(
            @PathVariable("id") UUID id,
            @Valid @RequestBody UpdateAssignmentStatusRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        User user = getAuthenticatedUser(principal);
        return ResponseEntity.ok(assignmentService.updateAssignmentStatus(id, request, user));
    }

    @PostMapping("/my-assignments/{id}/updates")
    @Operation(summary = "Submit a response progress and location update for an authorized assignment")
    public ResponseEntity<AssignmentResponse> addResponseUpdate(
            @PathVariable("id") UUID id,
            @Valid @RequestBody ResponseUpdateRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        User user = getAuthenticatedUser(principal);
        return ResponseEntity.ok(assignmentService.addResponseUpdate(id, request, user));
    }

    @PatchMapping("/team/status")
    @Operation(summary = "Update team availability status (only for authorized team members)")
    public ResponseEntity<TeamResponse> updateTeamStatus(
            @Valid @RequestBody UpdateTeamStatusRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        List<UUID> teamIds = teamService.getTeamIdsForUser(principal.getId());
        if (teamIds.isEmpty()) {
            throw new UnauthorizedActionException("You are not currently assigned to any active responder team.");
        }
        User user = getAuthenticatedUser(principal);
        // Update first authorized team
        UUID teamId = teamIds.get(0);
        return ResponseEntity.ok(teamService.updateTeamStatus(teamId, request.availabilityStatus(), user));
    }

    private User getAuthenticatedUser(UserPrincipal principal) {
        return userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + principal.getId()));
    }

    @GetMapping("/notifications")
    @Operation(summary = "Retrieve in-app notifications for the authenticated responder")
    public ResponseEntity<PagedResponse<NotificationResponse>> getNotifications(
            @AuthenticationPrincipal UserPrincipal principal,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(PagedResponse.from(notificationService.getNotificationsForUser(principal.getId(), pageable)));
    }

    @PatchMapping("/notifications/{id}/read")
    @Operation(summary = "Mark an in-app notification as read")
    public ResponseEntity<NotificationResponse> markNotificationAsRead(
            @PathVariable("id") UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(notificationService.markAsRead(id, principal.getId()));
    }
}
