package com.disaster.response.controller;

import com.disaster.response.config.UserPrincipal;
import com.disaster.response.domain.entity.User;
import com.disaster.response.domain.enums.AvailabilityStatus;
import com.disaster.response.domain.enums.UserRole;
import com.disaster.response.dto.auth.UserResponse;
import com.disaster.response.dto.common.PagedResponse;
import com.disaster.response.dto.instruction.SafetyInstructionRequest;
import com.disaster.response.dto.instruction.SafetyInstructionResponse;
import com.disaster.response.dto.report.AuditLogResponse;
import com.disaster.response.dto.report.DataSourceStatusResponse;
import com.disaster.response.dto.report.ResponsePerformanceReport;
import com.disaster.response.dto.risk.RegionRiskResponse;
import com.disaster.response.dto.risk.RiskModelInfoResponse;
import com.disaster.response.dto.team.CreateTeamRequest;
import com.disaster.response.dto.team.TeamMembersRequest;
import com.disaster.response.dto.team.TeamResponse;
import com.disaster.response.dto.team.UpdateTeamRequest;
import com.disaster.response.exception.ResourceNotFoundException;
import com.disaster.response.repository.UserRepository;
import com.disaster.response.service.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin")
@Tag(name = "Admin Operations & Management", description = "Team management, user administration, safety instructions, audit logs, and reports")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMIN')")
public class AdminManagementController {

    private final TeamService teamService;
    private final UserService userService;
    private final RiskAssessmentService riskAssessmentService;
    private final SafetyInstructionService instructionService;
    private final ReportingService reportingService;
    private final AuditService auditService;
    private final UserRepository userRepository;

    public AdminManagementController(
            TeamService teamService,
            UserService userService,
            RiskAssessmentService riskAssessmentService,
            SafetyInstructionService instructionService,
            ReportingService reportingService,
            AuditService auditService,
            UserRepository userRepository) {
        this.teamService = teamService;
        this.userService = userService;
        this.riskAssessmentService = riskAssessmentService;
        this.instructionService = instructionService;
        this.reportingService = reportingService;
        this.auditService = auditService;
        this.userRepository = userRepository;
    }

    // --- Teams ---
    @GetMapping("/teams")
    @Operation(summary = "List all responder teams with optional status filter")
    public ResponseEntity<PagedResponse<TeamResponse>> getTeams(
            @RequestParam(name = "status", required = false) AvailabilityStatus status,
            @PageableDefault(size = 20, sort = "name") Pageable pageable) {
        return ResponseEntity.ok(PagedResponse.from(teamService.getAllTeams(status, pageable)));
    }

    @PostMapping("/teams")
    @Operation(summary = "Create a new responder team with capabilities and base location")
    public ResponseEntity<TeamResponse> createTeam(
            @Valid @RequestBody CreateTeamRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        User admin = getAuthenticatedUser(principal);
        return ResponseEntity.status(HttpStatus.CREATED).body(teamService.createTeam(request, admin));
    }

    @PatchMapping("/teams/{id}")
    @Operation(summary = "Update responder team details")
    public ResponseEntity<TeamResponse> updateTeam(
            @PathVariable("id") UUID id,
            @Valid @RequestBody UpdateTeamRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        User admin = getAuthenticatedUser(principal);
        return ResponseEntity.ok(teamService.updateTeam(id, request, admin));
    }

    @PutMapping("/teams/{id}/members")
    @Operation(summary = "Set or replace responder team members")
    public ResponseEntity<TeamResponse> setTeamMembers(
            @PathVariable("id") UUID id,
            @Valid @RequestBody TeamMembersRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        User admin = getAuthenticatedUser(principal);
        return ResponseEntity.ok(teamService.setTeamMembers(id, request.userIds(), admin));
    }

    // --- Users ---
    @GetMapping("/users")
    @Operation(summary = "List system users with optional role filter")
    public ResponseEntity<PagedResponse<UserResponse>> getUsers(
            @RequestParam(name = "role", required = false) UserRole role,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(PagedResponse.from(userService.getAllUsers(role, pageable)));
    }

    @PatchMapping("/users/{id}")
    @Operation(summary = "Update user profile details or active status")
    public ResponseEntity<UserResponse> updateUser(
            @PathVariable("id") UUID id,
            @RequestBody Map<String, Object> updates,
            @AuthenticationPrincipal UserPrincipal principal) {
        User admin = getAuthenticatedUser(principal);
        String name = (String) updates.get("name");
        String phone = (String) updates.get("phone");
        Boolean active = (Boolean) updates.get("active");
        return ResponseEntity.ok(userService.updateUser(id, name, phone, active, admin));
    }

    @PatchMapping("/users/{id}/role")
    @Operation(summary = "Change a user's system role (ADMIN, RESPONDER, USER)")
    public ResponseEntity<UserResponse> updateUserRole(
            @PathVariable("id") UUID id,
            @RequestBody Map<String, String> request,
            @AuthenticationPrincipal UserPrincipal principal) {
        UserRole newRole = UserRole.valueOf(request.get("role"));
        User admin = getAuthenticatedUser(principal);
        return ResponseEntity.ok(userService.updateUserRole(id, newRole, admin));
    }

    // --- Risks ---
    @GetMapping("/risks/regions")
    @Operation(summary = "View system-wide regional disaster risk assessments")
    public ResponseEntity<List<RegionRiskResponse>> getRegionalRisks() {
        return ResponseEntity.ok(riskAssessmentService.getAllRegionsWithLatestRisk());
    }

    @GetMapping("/risks/model-info")
    @Operation(summary = "View active ML model registry metadata and health status")
    public ResponseEntity<RiskModelInfoResponse> getModelInfo() {
        return ResponseEntity.ok(riskAssessmentService.getModelInfo());
    }

    // --- Safety Instructions ---
    @PostMapping("/safety-instructions")
    @Operation(summary = "Create a new safety instruction guideline")
    public ResponseEntity<SafetyInstructionResponse> createSafetyInstruction(
            @Valid @RequestBody SafetyInstructionRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        User admin = getAuthenticatedUser(principal);
        return ResponseEntity.status(HttpStatus.CREATED).body(instructionService.createInstruction(request, admin));
    }

    @PatchMapping("/safety-instructions/{id}")
    @Operation(summary = "Update an existing safety instruction guideline")
    public ResponseEntity<SafetyInstructionResponse> updateSafetyInstruction(
            @PathVariable("id") UUID id,
            @Valid @RequestBody SafetyInstructionRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        User admin = getAuthenticatedUser(principal);
        return ResponseEntity.ok(instructionService.updateInstruction(id, request, admin));
    }

    private User getAuthenticatedUser(UserPrincipal principal) {
        return userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + principal.getId()));
    }

    // --- Reports & Logs ---
    @GetMapping("/reports/response-performance")
    @Operation(summary = "View aggregate emergency response performance report")
    public ResponseEntity<ResponsePerformanceReport> getPerformanceReport() {
        return ResponseEntity.ok(reportingService.getPerformanceReport());
    }

    @GetMapping("/audit-logs")
    @Operation(summary = "View immutable system audit logs")
    public ResponseEntity<PagedResponse<AuditLogResponse>> getAuditLogs(
            @PageableDefault(size = 20, sort = "timestamp", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(PagedResponse.from(auditService.getAuditLogs(pageable)));
    }

    @GetMapping("/data-sources/status")
    @Operation(summary = "View data-source provider freshness and ingestion status")
    public ResponseEntity<DataSourceStatusResponse> getDataSourceStatus() {
        return ResponseEntity.ok(reportingService.getDataSourceStatus());
    }
}
