package com.disaster.response.service;

import com.disaster.response.domain.entity.WeatherForecast;
import com.disaster.response.domain.entity.WeatherObservation;
import com.disaster.response.dto.weather.CurrentWeatherResponse;
import com.disaster.response.dto.weather.WeatherForecastResponse;
import com.disaster.response.exception.ResourceNotFoundException;
import com.disaster.response.gis.GisUtil;
import com.disaster.response.integration.weather.WeatherDataProvider;
import com.disaster.response.repository.WeatherForecastRepository;
import com.disaster.response.repository.WeatherObservationRepository;
import org.locationtech.jts.geom.Point;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
public class WeatherService {

    private static final Logger log = LoggerFactory.getLogger(WeatherService.class);

    private final WeatherDataProvider weatherProvider;
    private final WeatherObservationRepository observationRepository;
    private final WeatherForecastRepository forecastRepository;

    public WeatherService(
            WeatherDataProvider weatherProvider,
            WeatherObservationRepository observationRepository,
            WeatherForecastRepository forecastRepository) {
        this.weatherProvider = weatherProvider;
        this.observationRepository = observationRepository;
        this.forecastRepository = forecastRepository;
    }

    @Transactional
    public CurrentWeatherResponse getCurrentWeather(double latitude, double longitude) {
        GisUtil.validateCoordinates(latitude, longitude);
        Point point = GisUtil.createPoint(latitude, longitude);

        // 1. Attempt live provider fetch
        Optional<WeatherObservation> liveObs = weatherProvider.fetchCurrentObservation(point);
        if (liveObs.isPresent()) {
            WeatherObservation saved = observationRepository.save(liveObs.get());
            return CurrentWeatherResponse.from(saved, false);
        }

        // 2. Fall back to nearest stored observation in database, explicitly marking as STALE
        List<WeatherObservation> nearest = observationRepository.findNearestObservations(point, PageRequest.of(0, 1));
        if (!nearest.isEmpty()) {
            log.info("Returning cached/stale observation for point ({}, {})", latitude, longitude);
            return CurrentWeatherResponse.from(nearest.get(0), true);
        }

        throw new ResourceNotFoundException("No weather observations currently available for location (" + latitude + ", " + longitude + ")");
    }

    @Transactional
    public List<WeatherForecastResponse> getWeatherForecast(double latitude, double longitude) {
        GisUtil.validateCoordinates(latitude, longitude);
        Point point = GisUtil.createPoint(latitude, longitude);

        // 1. Attempt live provider forecast fetch
        List<WeatherForecast> liveForecasts = weatherProvider.fetchForecast(point);
        if (!liveForecasts.isEmpty()) {
            List<WeatherForecast> saved = forecastRepository.saveAll(liveForecasts);
            return saved.stream().map(WeatherForecastResponse::from).toList();
        }

        // 2. Fall back to nearest database forecasts
        List<WeatherForecast> cached = forecastRepository.findNearestForecasts(point, Instant.now(), PageRequest.of(0, 12));
        if (!cached.isEmpty()) {
            return cached.stream().map(WeatherForecastResponse::from).toList();
        }

        return List.of();
    }
}
