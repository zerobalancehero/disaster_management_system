package com.disaster.response.dto.weather;

import com.disaster.response.domain.entity.WeatherObservation;
import com.disaster.response.domain.enums.WeatherCondition;
import com.disaster.response.dto.common.GeoPointDto;
import com.disaster.response.gis.GisUtil;

import java.time.Duration;
import java.time.Instant;

public record CurrentWeatherResponse(
    String source,
    GeoPointDto location,
    Instant observationTime,
    Double temperatureCelsius,
    Double humidityPercent,
    Double rainfallMm,
    Double windSpeedKmh,
    WeatherCondition weatherCondition,
    boolean isStale,
    String freshnessNote
) {
    public static CurrentWeatherResponse from(WeatherObservation obs, boolean forceStale) {
        if (obs == null) {
            return null;
        }
        boolean stale = forceStale || (obs.getObservationTime() != null &&
                Duration.between(obs.getObservationTime(), Instant.now()).toHours() >= 3);
        String note = stale ? "Data is older than 3 hours or provider is currently unreachable. Not guaranteed live." : "Live data";

        GeoPointDto loc = obs.getCoordinates() != null
                ? GeoPointDto.of(GisUtil.getLatitude(obs.getCoordinates()), GisUtil.getLongitude(obs.getCoordinates()))
                : null;

        return new CurrentWeatherResponse(
            obs.getSource(),
            loc,
            obs.getObservationTime(),
            obs.getTemperature(),
            obs.getHumidity(),
            obs.getRainfall(),
            obs.getWindSpeed(),
            obs.getWeatherCondition(),
            stale,
            note
        );
    }
}
