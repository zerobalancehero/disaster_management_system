package com.disaster.response.job;

import com.disaster.response.service.AlertService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.scheduling.enabled", havingValue = "true", matchIfMissing = true)
public class AlertIngestionJob {

    private static final Logger log = LoggerFactory.getLogger(AlertIngestionJob.class);

    private final AlertService alertService;

    public AlertIngestionJob(AlertService alertService) {
        this.alertService = alertService;
    }

    /**
     * Periodically polls configured disaster alert providers (e.g. USGS, GDACS).
     * Runs every 15 minutes with an initial 10-second delay.
     */
    @Scheduled(initialDelay = 10000, fixedRateString = "${app.jobs.alerts.rate-ms:900000}")
    public void runAlertIngestion() {
        log.info("Starting scheduled disaster alert ingestion job...");
        try {
            int ingested = alertService.ingestFromAllProviders();
            log.info("Scheduled alert ingestion completed. New alerts ingested: {}", ingested);
        } catch (Exception e) {
            log.error("Scheduled alert ingestion failed: {}", e.getMessage(), e);
        }
    }
}
