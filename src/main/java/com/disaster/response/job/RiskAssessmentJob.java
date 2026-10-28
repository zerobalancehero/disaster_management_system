package com.disaster.response.job;

import com.disaster.response.domain.enums.DisasterType;
import com.disaster.response.domain.enums.PredictionHorizon;
import com.disaster.response.service.RiskAssessmentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Periodically triggers automated AI disaster risk assessments across all monitored geographical regions.
 */
@Component
@ConditionalOnProperty(name = "app.scheduling.enabled", havingValue = "true", matchIfMissing = true)
public class RiskAssessmentJob {

    private static final Logger log = LoggerFactory.getLogger(RiskAssessmentJob.class);

    private final RiskAssessmentService riskAssessmentService;

    public RiskAssessmentJob(RiskAssessmentService riskAssessmentService) {
        this.riskAssessmentService = riskAssessmentService;
    }

    /**
     * Executes scheduled AI inference across all monitored regions for primary hazards (FLOOD, CYCLONE).
     * Runs every 30 minutes with an initial 20-second delay.
     */
    @Scheduled(initialDelay = 20000, fixedRateString = "${app.jobs.risks.rate-ms:1800000}")
    public void runPeriodicRiskAssessments() {
        log.info("Starting scheduled regional disaster risk assessment job...");
        try {
            int floodCount = riskAssessmentService.assessAllRegionsRisk(DisasterType.FLOOD, PredictionHorizon.NEXT_24H).size();
            int cycloneCount = riskAssessmentService.assessAllRegionsRisk(DisasterType.CYCLONE, PredictionHorizon.NEXT_24H).size();
            log.info("Scheduled risk assessment completed. Calculated {} flood and {} cyclone regional risk predictions.",
                    floodCount, cycloneCount);
        } catch (Exception e) {
            log.error("Scheduled regional risk assessment job failed: {}", e.getMessage(), e);
        }
    }
}
