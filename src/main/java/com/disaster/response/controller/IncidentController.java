package com.disaster.response.controller;

import com.disaster.response.config.UserPrincipal;
import com.disaster.response.domain.entity.User;
import com.disaster.response.dto.common.PagedResponse;
import com.disaster.response.dto.incident.IncidentReportRequest;
import com.disaster.response.dto.incident.IncidentResponse;
import com.disaster.response.exception.ResourceNotFoundException;
import com.disaster.response.repository.UserRepository;
import com.disaster.response.service.IncidentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/incidents")
@Tag(name = "Incident Reports", description = "Public / User incident reporting and personal report tracking")
@SecurityRequirement(name = "bearerAuth")
public class IncidentController {

    private final IncidentService incidentService;
    private final UserRepository userRepository;

    public IncidentController(IncidentService incidentService, UserRepository userRepository) {
        this.incidentService = incidentService;
        this.userRepository = userRepository;
    }

    @PostMapping("/reports")
    @Operation(summary = "Submit a new disaster incident report")
    public ResponseEntity<IncidentResponse> submitReport(
            @Valid @RequestBody IncidentReportRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + principal.getId()));
        IncidentResponse response = incidentService.reportIncident(request, user);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/my-reports")
    @Operation(summary = "View the status of reports submitted by the authenticated user")
    public ResponseEntity<PagedResponse<IncidentResponse>> getMyReports(
            @AuthenticationPrincipal UserPrincipal principal,
            @PageableDefault(size = 20, sort = "reportedTime", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(PagedResponse.from(incidentService.getMyReports(principal.getId(), pageable)));
    }

    @GetMapping("/my-reports/{id}")
    @Operation(summary = "View details of an authenticated user's specific report")
    public ResponseEntity<IncidentResponse> getMyReportById(
            @PathVariable("id") UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(incidentService.getMyReportById(id, principal.getId()));
    }
}
