package com.disaster.response.integration.weather;

import com.disaster.response.domain.entity.WeatherForecast;
import com.disaster.response.domain.entity.WeatherObservation;
import com.disaster.response.domain.enums.WeatherCondition;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.locationtech.jts.geom.Point;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
public class OpenMeteoWeatherProvider implements WeatherDataProvider {

    private static final Logger log = LoggerFactory.getLogger(OpenMeteoWeatherProvider.class);

    private final String baseUrl;
    private final boolean enabled;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public OpenMeteoWeatherProvider(
            @Value("${app.integrations.open-meteo.base-url:https://api.open-meteo.com/v1/forecast}") String baseUrl,
            @Value("${app.integrations.open-meteo.enabled:true}") boolean enabled,
            @Value("${app.integrations.open-meteo.timeout-seconds:5}") int timeoutSeconds,
            ObjectMapper objectMapper) {
        this.baseUrl = baseUrl;
        this.enabled = enabled;
        this.objectMapper = objectMapper;

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(timeoutSeconds * 1000);
        factory.setReadTimeout(timeoutSeconds * 1000);

        this.restClient = RestClient.builder()
                .requestFactory(factory)
                .build();
    }

    @Override
    public String getProviderName() {
        return "OPEN_METEO";
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public Optional<WeatherObservation> fetchCurrentObservation(Point coordinates) {
        if (!enabled) {
            log.debug("Open-Meteo weather provider is disabled.");
            return Optional.empty();
        }

        try {
            double lat = coordinates.getY();
            double lon = coordinates.getX();
            String url = String.format("%s?latitude=%.4f&longitude=%.4f&current=temperature_2m,relative_humidity_2m,precipitation,wind_speed_10m,weather_code",
                    baseUrl, lat, lon);

            String response = restClient.get()
                    .uri(url)
                    .retrieve()
                    .body(String.class);

            JsonNode root = objectMapper.readTree(response);
            JsonNode current = root.path("current");
            if (current.isMissingNode()) {
                return Optional.empty();
            }

            Double temp = current.path("temperature_2m").asDouble();
            Double humidity = current.path("relative_humidity_2m").asDouble();
            Double rain = current.path("precipitation").asDouble();
            Double wind = current.path("wind_speed_10m").asDouble();
            int weatherCode = current.path("weather_code").asInt(0);

            WeatherObservation obs = new WeatherObservation(
                    getProviderName(),
                    String.format("%.4f,%.4f", lat, lon),
                    coordinates,
                    Instant.now(),
                    temp,
                    humidity,
                    rain,
                    wind,
                    mapWeatherCode(weatherCode),
                    response
            );

            return Optional.of(obs);
        } catch (Exception e) {
            log.warn("Failed to fetch live weather from Open-Meteo: {}", e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public List<WeatherForecast> fetchForecast(Point coordinates) {
        if (!enabled) {
            return List.of();
        }

        try {
            double lat = coordinates.getY();
            double lon = coordinates.getX();
            String url = String.format("%s?latitude=%.4f&longitude=%.4f&hourly=temperature_2m,precipitation,wind_speed_10m,weather_code&forecast_days=2",
                    baseUrl, lat, lon);

            String response = restClient.get()
                    .uri(url)
                    .retrieve()
                    .body(String.class);

            JsonNode root = objectMapper.readTree(response);
            JsonNode hourly = root.path("hourly");
            JsonNode times = hourly.path("time");
            JsonNode temps = hourly.path("temperature_2m");
            JsonNode rains = hourly.path("precipitation");
            JsonNode winds = hourly.path("wind_speed_10m");
            JsonNode codes = hourly.path("weather_code");

            List<WeatherForecast> forecasts = new ArrayList<>();
            Instant issueTime = Instant.now();

            int count = Math.min(24, times.size());
            for (int i = 0; i < count; i += 3) { // every 3 hours
                Instant targetTime = Instant.parse(times.get(i).asText() + ":00Z");
                WeatherForecast fc = new WeatherForecast(
                        getProviderName(),
                        coordinates,
                        null,
                        issueTime,
                        targetTime,
                        temps.get(i).asDouble(),
                        rains.get(i).asDouble(),
                        winds.get(i).asDouble(),
                        mapWeatherCode(codes.get(i).asInt(0))
                );
                forecasts.add(fc);
            }

            return forecasts;
        } catch (Exception e) {
            log.warn("Failed to fetch forecasts from Open-Meteo: {}", e.getMessage());
            return List.of();
        }
    }

    private WeatherCondition mapWeatherCode(int code) {
        if (code == 0) return WeatherCondition.CLEAR;
        if (code <= 3) return WeatherCondition.CLOUDY;
        if (code >= 51 && code <= 67) return WeatherCondition.RAIN;
        if (code >= 71 && code <= 77) return WeatherCondition.SNOW;
        if (code >= 80 && code <= 82) return WeatherCondition.HEAVY_RAIN;
        if (code >= 95) return WeatherCondition.THUNDERSTORM;
        return WeatherCondition.CLOUDY;
    }
}
