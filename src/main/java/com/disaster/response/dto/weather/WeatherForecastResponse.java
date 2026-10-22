package com.disaster.response.dto.weather;

import com.disaster.response.domain.entity.WeatherForecast;
import com.disaster.response.domain.enums.WeatherCondition;
import com.disaster.response.dto.common.GeoPointDto;
import com.disaster.response.gis.GisUtil;

import java.time.Instant;
import java.util.UUID;

public record WeatherForecastResponse(
    UUID id,
    String source,
    GeoPointDto coordinates,
    UUID regionId,
    Instant forecastIssueTime,
    Instant forecastTargetTime,
    Double temperatureCelsius,
    Double rainfallMm,
    Double windSpeedKmh,
    WeatherCondition weatherCondition
) {
    public static WeatherForecastResponse from(WeatherForecast fc) {
        if (fc == null) {
            return null;
        }
        GeoPointDto loc = fc.getCoordinates() != null
                ? GeoPointDto.of(GisUtil.getLatitude(fc.getCoordinates()), GisUtil.getLongitude(fc.getCoordinates()))
                : null;

        return new WeatherForecastResponse(
            fc.getId(),
            fc.getSource(),
            loc,
            fc.getRegion() != null ? fc.getRegion().getId() : null,
            fc.getForecastIssueTime(),
            fc.getForecastTargetTime(),
            fc.getTemperature(),
            fc.getRainfall(),
            fc.getWindSpeed(),
            fc.getWeatherCondition()
        );
    }
}
