package com.disaster.response;

import com.disaster.response.domain.entity.WeatherObservation;
import com.disaster.response.domain.enums.WeatherCondition;
import com.disaster.response.dto.weather.CurrentWeatherResponse;
import com.disaster.response.exception.ResourceNotFoundException;
import com.disaster.response.gis.GisUtil;
import com.disaster.response.integration.weather.WeatherDataProvider;
import com.disaster.response.repository.WeatherForecastRepository;
import com.disaster.response.repository.WeatherObservationRepository;
import com.disaster.response.service.WeatherService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WeatherAndStaleDataTest {

    @Mock private WeatherDataProvider weatherProvider;
    @Mock private WeatherObservationRepository observationRepository;
    @Mock private WeatherForecastRepository forecastRepository;

    private WeatherService weatherService;

    @BeforeEach
    void setUp() {
        weatherService = new WeatherService(weatherProvider, observationRepository, forecastRepository);
    }

    @Test
    @DisplayName("Live Weather: Provider observation is returned with isStale=false")
    void testLiveWeatherObservationReturnedNonStale() {
        WeatherObservation liveObs = new WeatherObservation(
                "OPEN_METEO", "13.08,80.27", GisUtil.createPoint(13.08, 80.27), Instant.now(),
                30.5, 75.0, 12.0, 25.0, WeatherCondition.RAIN, "{}"
        );

        when(weatherProvider.fetchCurrentObservation(any())).thenReturn(Optional.of(liveObs));
        when(observationRepository.save(any(WeatherObservation.class))).thenAnswer(i -> i.getArgument(0));

        CurrentWeatherResponse response = weatherService.getCurrentWeather(13.08, 80.27);

        assertNotNull(response);
        assertFalse(response.isStale());
        assertEquals("Live data", response.freshnessNote());
        assertEquals(30.5, response.temperatureCelsius());
    }

    @Test
    @DisplayName("Rule 12: Stale weather data is reported honestly with isStale=true when provider is unreachable")
    void testStaleWeatherDataReportedHonestlyWhenProviderDown() {
        // Provider fails / returns empty
        when(weatherProvider.fetchCurrentObservation(any())).thenReturn(Optional.empty());

        // Stored observation from 5 hours ago exists in database
        WeatherObservation oldObs = new WeatherObservation(
                "OPEN_METEO", "13.08,80.27", GisUtil.createPoint(13.08, 80.27),
                Instant.now().minus(5, ChronoUnit.HOURS),
                28.0, 80.0, 0.0, 15.0, WeatherCondition.CLOUDY, "{}"
        );
        when(observationRepository.findNearestObservations(any(), any(Pageable.class))).thenReturn(List.of(oldObs));

        CurrentWeatherResponse response = weatherService.getCurrentWeather(13.08, 80.27);

        assertNotNull(response);
        assertTrue(response.isStale());
        assertTrue(response.freshnessNote().contains("Not guaranteed live"));
        assertEquals(28.0, response.temperatureCelsius());
    }

    @Test
    @DisplayName("Rule 13: Controlled error when no weather observation exists (never fabricate fake data)")
    void testNoFabricationWhenNoWeatherExists() {
        when(weatherProvider.fetchCurrentObservation(any())).thenReturn(Optional.empty());
        when(observationRepository.findNearestObservations(any(), any(Pageable.class))).thenReturn(List.of());

        assertThrows(ResourceNotFoundException.class, () ->
                weatherService.getCurrentWeather(13.08, 80.27)
        );
    }
}
