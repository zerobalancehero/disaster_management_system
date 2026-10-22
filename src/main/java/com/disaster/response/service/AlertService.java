package com.disaster.response.service;

import com.disaster.response.domain.entity.DataSourceRun;
import com.disaster.response.domain.entity.DisasterAlert;
import com.disaster.response.domain.enums.AlertStatus;
import com.disaster.response.dto.alert.DisasterAlertResponse;
import com.disaster.response.exception.ResourceNotFoundException;
import com.disaster.response.gis.GisUtil;
import com.disaster.response.integration.alert.AlertDataProvider;
import com.disaster.response.repository.DataSourceRunRepository;
import com.disaster.response.repository.DisasterAlertRepository;
import org.locationtech.jts.geom.Point;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class AlertService {

    private static final Logger log = LoggerFactory.getLogger(AlertService.class);

    private final DisasterAlertRepository alertRepository;
    private final List<AlertDataProvider> alertProviders;
    private final DataSourceRunRepository runRepository;

    public AlertService(
            DisasterAlertRepository alertRepository,
            List<AlertDataProvider> alertProviders,
            DataSourceRunRepository runRepository) {
        this.alertRepository = alertRepository;
        this.alertProviders = alertProviders;
        this.runRepository = runRepository;
    }

    @Transactional(readOnly = true)
    public List<DisasterAlertResponse> getActiveAlerts() {
        return alertRepository.findActiveAlerts(Instant.now())
                .stream()
                .map(DisasterAlertResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<DisasterAlertResponse> getActiveAlerts(Pageable pageable) {
        return alertRepository.findActiveAlerts(Instant.now(), pageable)
                .map(DisasterAlertResponse::from);
    }

    @Transactional(readOnly = true)
    public Page<DisasterAlertResponse> getHistoricalAlerts(Pageable pageable) {
        return alertRepository.findAll(pageable)
                .map(DisasterAlertResponse::from);
    }

    @Transactional(readOnly = true)
    public DisasterAlertResponse getAlertById(UUID alertId) {
        DisasterAlert alert = alertRepository.findById(alertId)
                .orElseThrow(() -> new ResourceNotFoundException("Disaster alert not found with ID: " + alertId));
        return DisasterAlertResponse.from(alert);
    }

    @Transactional(readOnly = true)
    public List<DisasterAlertResponse> getAlertsNearLocation(double latitude, double longitude) {
        Point point = GisUtil.createPoint(latitude, longitude);
        return alertRepository.findAlertsContainingPoint(point)
                .stream()
                .map(DisasterAlertResponse::from)
                .toList();
    }

    /**
     * Executes scheduled or on-demand alert ingestion across all configured providers.
     * Enforces deduplication using (source, externalSourceId).
     */
    @Transactional
    public int ingestFromAllProviders() {
        int totalIngested = 0;

        for (AlertDataProvider provider : alertProviders) {
            if (!provider.isEnabled()) {
                continue;
            }

            DataSourceRun run = new DataSourceRun(provider.getProviderName(), "ALERT_INGESTION");
            try {
                List<DisasterAlert> alerts = provider.fetchLatestAlerts();
                int newCount = 0;

                for (DisasterAlert alert : alerts) {
                    if (!alertRepository.existsBySourceAndExternalSourceId(alert.getSource(), alert.getExternalSourceId())) {
                        alertRepository.save(alert);
                        newCount++;
                    }
                }

                run.complete(newCount);
                totalIngested += newCount;
                log.info("Ingested {} new alerts from provider {}", newCount, provider.getProviderName());
            } catch (Exception e) {
                log.error("Failed alert ingestion for provider {}: {}", provider.getProviderName(), e.getMessage());
                run.fail(e.getMessage());
            } finally {
                runRepository.save(run);
            }
        }

        return totalIngested;
    }
}
