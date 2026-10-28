package com.disaster.response.controller;

import com.disaster.response.domain.enums.AlertSeverity;
import com.disaster.response.domain.enums.DisasterType;
import com.disaster.response.dto.alert.DisasterAlertResponse;
import com.disaster.response.dto.common.PagedResponse;
import com.disaster.response.dto.instruction.SafetyInstructionResponse;
import com.disaster.response.service.AlertService;
import com.disaster.response.service.SafetyInstructionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/alerts")
@Tag(name = "Disaster Alerts", description = "Active and historical disaster alerts and safety instructions")
@SecurityRequirement(name = "bearerAuth")
public class AlertController {

    private final AlertService alertService;
    private final SafetyInstructionService instructionService;

    public AlertController(AlertService alertService, SafetyInstructionService instructionService) {
        this.alertService = alertService;
        this.instructionService = instructionService;
    }

    @GetMapping("/active")
    @Operation(summary = "View currently active disaster alerts")
    public ResponseEntity<List<DisasterAlertResponse>> getActiveAlerts() {
        List<DisasterAlertResponse> alerts = alertService.getActiveAlerts();
        return ResponseEntity.ok(alerts);
    }

    @GetMapping("/history")
    @Operation(summary = "View paginated historical disaster alerts")
    public ResponseEntity<PagedResponse<DisasterAlertResponse>> getAlertHistory(
            @PageableDefault(size = 20, sort = "issueTime", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(PagedResponse.from(alertService.getHistoricalAlerts(pageable)));
    }

    @GetMapping("/safety-instructions")
    @Operation(summary = "View safety instructions tailored to disaster hazard type and severity")
    public ResponseEntity<List<SafetyInstructionResponse>> getSafetyInstructions(
            @RequestParam("disasterType") DisasterType disasterType,
            @RequestParam(name = "severity", required = false) AlertSeverity severity,
            @RequestParam(name = "lang", defaultValue = "en") String lang) {
        List<SafetyInstructionResponse> instructions =
                instructionService.getApplicableInstructions(disasterType, severity, lang);
        return ResponseEntity.ok(instructions);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get disaster alert details by ID")
    public ResponseEntity<DisasterAlertResponse> getAlertById(@PathVariable("id") UUID id) {
        return ResponseEntity.ok(alertService.getAlertById(id));
    }

    @GetMapping("/{id}/safety-instructions")
    @Operation(summary = "View safety instructions relevant to a specific disaster alert")
    public ResponseEntity<List<SafetyInstructionResponse>> getAlertSafetyInstructions(
            @PathVariable("id") UUID id,
            @RequestParam(name = "lang", defaultValue = "en") String lang) {
        DisasterAlertResponse alert = alertService.getAlertById(id);
        List<SafetyInstructionResponse> instructions =
                instructionService.getApplicableInstructions(alert.disasterType(), alert.severity(), lang);
        return ResponseEntity.ok(instructions);
    }
}
