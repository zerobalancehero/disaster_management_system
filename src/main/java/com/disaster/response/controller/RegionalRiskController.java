package com.disaster.response.controller;

import com.disaster.response.dto.common.PagedResponse;
import com.disaster.response.dto.risk.RegionRiskResponse;
import com.disaster.response.dto.risk.RiskAssessmentResponse;
import com.disaster.response.service.RiskAssessmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/risks")
@Tag(name = "Regional Risk", description = "AI-based regional disaster risk assessments and prediction history")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasAnyRole('RESPONDER', 'ADMIN')")
public class RegionalRiskController {

    private final RiskAssessmentService riskAssessmentService;

    public RegionalRiskController(RiskAssessmentService riskAssessmentService) {
        this.riskAssessmentService = riskAssessmentService;
    }

    @GetMapping("/regions")
    @Operation(summary = "View regional disaster risk assessments across all monitored areas")
    public ResponseEntity<List<RegionRiskResponse>> getRegionalRisks() {
        return ResponseEntity.ok(riskAssessmentService.getAllRegionsWithLatestRisk());
    }

    @GetMapping("/regions/{regionId}")
    @Operation(summary = "View detailed risk assessment for a specific region")
    public ResponseEntity<RegionRiskResponse> getRegionRisk(@PathVariable("regionId") UUID regionId) {
        return ResponseEntity.ok(riskAssessmentService.getRegionRisk(regionId));
    }

    @GetMapping("/history")
    @Operation(summary = "View historical AI predictions for a specific region")
    public ResponseEntity<PagedResponse<RiskAssessmentResponse>> getRiskHistory(
            @RequestParam("regionId") UUID regionId,
            @PageableDefault(size = 20, sort = "predictionTimestamp", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(PagedResponse.from(riskAssessmentService.getRiskHistory(regionId, pageable)));
    }
}
