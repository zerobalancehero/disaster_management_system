package com.disaster.response.integration.weather;

import com.disaster.response.domain.entity.WeatherForecast;
import com.disaster.response.domain.entity.WeatherObservation;
import org.locationtech.jts.geom.Point;

import java.util.List;
import java.util.Optional;

public interface WeatherDataProvider {

    String getProviderName();

    boolean isEnabled();

    Optional<WeatherObservation> fetchCurrentObservation(Point coordinates);

    List<WeatherForecast> fetchForecast(Point coordinates);
}
