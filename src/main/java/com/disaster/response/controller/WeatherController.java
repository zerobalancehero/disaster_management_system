package com.disaster.response.controller;

import com.disaster.response.dto.weather.CurrentWeatherResponse;
import com.disaster.response.dto.weather.WeatherForecastResponse;
import com.disaster.response.service.WeatherService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/weather")
@Tag(name = "Weather", description = "Current weather and forecasts with stale data indicators")
@SecurityRequirement(name = "bearerAuth")
public class WeatherController {

    private final WeatherService weatherService;

    public WeatherController(WeatherService weatherService) {
        this.weatherService = weatherService;
    }

    @GetMapping("/current")
    @Operation(summary = "Retrieve current weather observation for coordinates")
    public ResponseEntity<CurrentWeatherResponse> getCurrentWeather(
            @Parameter(description = "Latitude [-90.0, 90.0]") @RequestParam("lat") double lat,
            @Parameter(description = "Longitude [-180.0, 180.0]") @RequestParam("lon") double lon) {
        CurrentWeatherResponse response = weatherService.getCurrentWeather(lat, lon);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/forecast")
    @Operation(summary = "Retrieve weather forecast for coordinates")
    public ResponseEntity<List<WeatherForecastResponse>> getWeatherForecast(
            @Parameter(description = "Latitude [-90.0, 90.0]") @RequestParam("lat") double lat,
            @Parameter(description = "Longitude [-180.0, 180.0]") @RequestParam("lon") double lon) {
        List<WeatherForecastResponse> response = weatherService.getWeatherForecast(lat, lon);
        return ResponseEntity.ok(response);
    }
}
