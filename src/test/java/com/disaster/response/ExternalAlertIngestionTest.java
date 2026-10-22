package com.disaster.response;

import com.disaster.response.domain.entity.DataSourceRun;
import com.disaster.response.domain.entity.DisasterAlert;
import com.disaster.response.domain.enums.AlertSeverity;
import com.disaster.response.domain.enums.AlertStatus;
import com.disaster.response.domain.enums.DisasterType;
import com.disaster.response.gis.GisUtil;
import com.disaster.response.integration.alert.AlertDataProvider;
import com.disaster.response.repository.DataSourceRunRepository;
import com.disaster.response.repository.DisasterAlertRepository;
import com.disaster.response.service.AlertService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExternalAlertIngestionTest {

    @Mock private DisasterAlertRepository alertRepository;
    @Mock private DataSourceRunRepository runRepository;
    @Mock private AlertDataProvider alertProvider;

    private AlertService alertService;

    @BeforeEach
    void setUp() {
        alertService = new AlertService(alertRepository, List.of(alertProvider), runRepository);
    }

    @Test
    @DisplayName("Deduplication: Duplicate external alerts are not ingested twice")
    void testDuplicateExternalAlertsAreNotIngestedTwice() {
        when(alertProvider.isEnabled()).thenReturn(true);
        when(alertProvider.getProviderName()).thenReturn("USGS");

        DisasterAlert alert1 = new DisasterAlert(
                "USGS", "us7000abcd", DisasterType.EARTHQUAKE, AlertSeverity.SEVERE,
                "M 6.2 Earthquake", "Details", GisUtil.createPoint(13.0, 80.0),
                Instant.now(), Instant.now().plusSeconds(86400), AlertStatus.ACTIVE, "{}"
        );

        when(alertProvider.fetchLatestAlerts()).thenReturn(List.of(alert1));
        // Simulate alert already exists in database
        when(alertRepository.existsBySourceAndExternalSourceId("USGS", "us7000abcd")).thenReturn(true);

        int count = alertService.ingestFromAllProviders();

        assertEquals(0, count);
        // Verify alertRepository.save was NEVER called for the duplicate alert
        verify(alertRepository, never()).save(any(DisasterAlert.class));
        verify(runRepository, times(1)).save(any(DataSourceRun.class));
    }

    @Test
    @DisplayName("New Alert Ingestion: Saves novel alerts and records successful data source run")
    void testNewAlertIsSavedSuccessfully() {
        when(alertProvider.isEnabled()).thenReturn(true);
        when(alertProvider.getProviderName()).thenReturn("USGS");

        DisasterAlert alert1 = new DisasterAlert(
                "USGS", "us7000new1", DisasterType.EARTHQUAKE, AlertSeverity.SEVERE,
                "M 6.5 Earthquake", "Details", GisUtil.createPoint(13.0, 80.0),
                Instant.now(), Instant.now().plusSeconds(86400), AlertStatus.ACTIVE, "{}"
        );

        when(alertProvider.fetchLatestAlerts()).thenReturn(List.of(alert1));
        when(alertRepository.existsBySourceAndExternalSourceId("USGS", "us7000new1")).thenReturn(false);

        int count = alertService.ingestFromAllProviders();

        assertEquals(1, count);
        verify(alertRepository, times(1)).save(alert1);
        verify(runRepository, times(1)).save(any(DataSourceRun.class));
    }
}
