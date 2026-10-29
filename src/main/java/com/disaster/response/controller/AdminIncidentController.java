package com.disaster.response.controller;

import com.disaster.response.config.UserPrincipal;
import com.disaster.response.domain.entity.User;
import com.disaster.response.domain.enums.VerificationStatus;
import com.disaster.response.dto.assignment.AssignmentResponse;
import com.disaster.response.dto.common.PagedResponse;
import com.disaster.response.dto.incident.IncidentResponse;
import com.disaster.response.dto.incident.ReassignIncidentRequest;
import com.disaster.response.dto.incident.RejectIncidentRequest;
import com.disaster.response.dto.incident.VerifyIncidentRequest;
import com.disaster.response.dto.recommendation.RecommendationRejectRequest;
import com.disaster.response.dto.recommendation.RecommendationResponse;
import com.disaster.response.dto.report.AdminDashboardResponse;
import com.disaster.response.exception.ResourceNotFoundException;
import com.disaster.response.repository.UserRepository;
import com.disaster.response.service.AssignmentService;
import com.disaster.response.service.IncidentService;
import com.disaster.response.service.RecommendationService;
import com.disaster.response.service.ReportingService;
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
@RequestMapping("/api/v1/admin")
@Tag(name = "Admin Incident & Dispatch", description = "Incident verification, recommendation generation, and dispatch workflows")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMIN')")
public class AdminIncidentController {

    private final IncidentService incidentService;
    private final RecommendationService recommendationService;
    private final AssignmentService assignmentService;
    private final ReportingService reportingService;
    private final UserRepository userRepository;

    public AdminIncidentController(
            IncidentService incidentService,
            RecommendationService recommendationService,
            AssignmentService assignmentService,
            ReportingService reportingService,
            UserRepository userRepository) {
        this.incidentService = incidentService;
        this.recommendationService = recommendationService;
        this.assignmentService = assignmentService;
        this.reportingService = reportingService;
        this.userRepository = userRepository;
    }

    @GetMapping("/dashboard")
    @Operation(summary = "System-wide administrative dashboard metrics and status summary")
    public ResponseEntity<AdminDashboardResponse> getDashboard() {
        return ResponseEntity.ok(reportingService.getAdminDashboard());
    }

    @GetMapping("/incidents")
    @Operation(summary = "View all system-wide incidents with optional verification status filter")
    public ResponseEntity<PagedResponse<IncidentResponse>> getIncidents(
            @RequestParam(name = "status", required = false) VerificationStatus status,
            @PageableDefault(size = 20, sort = "reportedTime", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(PagedResponse.from(incidentService.getAllIncidents(status, pageable)));
    }

    @GetMapping("/incidents/pending")
    @Operation(summary = "View incidents awaiting administrator verification")
    public ResponseEntity<PagedResponse<IncidentResponse>> getPendingIncidents(
            @PageableDefault(size = 20, sort = "reportedTime", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(PagedResponse.from(incidentService.getPendingIncidents(pageable)));
    }

    @GetMapping("/incidents/{id}")
    @Operation(summary = "Get full details of any incident by ID")
    public ResponseEntity<IncidentResponse> getIncidentById(@PathVariable("id") UUID id) {
        return ResponseEntity.ok(incidentService.getIncidentById(id));
    }

    @PatchMapping("/incidents/{id}/verify")
    @Operation(summary = "Verify an incident, making it eligible for team recommendations")
    public ResponseEntity<IncidentResponse> verifyIncident(
            @PathVariable("id") UUID id,
            @Valid @RequestBody(required = false) VerifyIncidentRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        User admin = getAuthenticatedUser(principal);
        return ResponseEntity.ok(incidentService.verifyIncident(id, request, admin));
    }

    @PatchMapping("/incidents/{id}/reject")
    @Operation(summary = "Reject an incident report with mandatory reason")
    public ResponseEntity<IncidentResponse> rejectIncident(
            @PathVariable("id") UUID id,
            @Valid @RequestBody RejectIncidentRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        User admin = getAuthenticatedUser(principal);
        return ResponseEntity.ok(incidentService.rejectIncident(id, request, admin));
    }

    @PostMapping("/incidents/{id}/recommendations")
    @Operation(summary = "Trigger recommendation engine for verified incident to produce ranked eligible teams")
    public ResponseEntity<List<RecommendationResponse>> generateRecommendations(
            @PathVariable("id") UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {
        User admin = getAuthenticatedUser(principal);
        return ResponseEntity.ok(recommendationService.generateRecommendations(id, admin));
    }

    @GetMapping("/incidents/{id}/recommendations")
    @Operation(summary = "View current ranked team recommendations for an incident")
    public ResponseEntity<List<RecommendationResponse>> getRecommendations(@PathVariable("id") UUID id) {
        return ResponseEntity.ok(recommendationService.getRecommendations(id));
    }

    @PostMapping("/assignments/{recommendationId}/approve")
    @Operation(summary = "Approve a team recommendation and create confirmed dispatch assignment transactionally")
    public ResponseEntity<AssignmentResponse> approveRecommendation(
            @PathVariable("recommendationId") UUID recommendationId,
            @AuthenticationPrincipal UserPrincipal principal) {
        User admin = getAuthenticatedUser(principal);
        return ResponseEntity.ok(assignmentService.approveRecommendationAndDispatch(recommendationId, admin));
    }

    @PostMapping("/assignments/{recommendationId}/reject")
    @Operation(summary = "Reject a team recommendation (ensures team is never dispatched)")
    public ResponseEntity<RecommendationResponse> rejectRecommendation(
            @PathVariable("recommendationId") UUID recommendationId,
            @Valid @RequestBody RecommendationRejectRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        User admin = getAuthenticatedUser(principal);
        return ResponseEntity
                .ok(recommendationService.rejectRecommendation(recommendationId, request.rejectionReason(), admin));
    }

    @PostMapping("/incidents/{id}/reassign")
    @Operation(summary = "Cancel active assignment and reset incident for reassignment")
    public ResponseEntity<AssignmentResponse> reassignIncident(
            @PathVariable("id") UUID id,
            @Valid @RequestBody ReassignIncidentRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        User admin = getAuthenticatedUser(principal);
        return ResponseEntity.ok(assignmentService.reassignIncident(id, request.reason(), admin));
    }

    private User getAuthenticatedUser(UserPrincipal principal) {
        return userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + principal.getId()));
    }
}
