package com.disaster.response.repository;

import com.disaster.response.domain.entity.WeatherForecast;
import org.locationtech.jts.geom.Point;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface WeatherForecastRepository extends JpaRepository<WeatherForecast, UUID> {

    @Query("SELECT f FROM WeatherForecast f WHERE f.forecastTargetTime >= :fromTime ORDER BY ST_Distance(f.coordinates, :point) ASC, f.forecastTargetTime ASC")
    List<WeatherForecast> findNearestForecasts(@Param("point") Point point, @Param("fromTime") Instant fromTime, Pageable pageable);

    List<WeatherForecast> findByRegionIdAndForecastTargetTimeAfterOrderByForecastTargetTimeAsc(UUID regionId, Instant fromTime);
}
