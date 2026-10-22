package com.disaster.response.repository;

import com.disaster.response.domain.entity.WeatherObservation;
import org.locationtech.jts.geom.Point;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WeatherObservationRepository extends JpaRepository<WeatherObservation, UUID> {

    @Query("SELECT w FROM WeatherObservation w ORDER BY ST_Distance(w.coordinates, :point) ASC, w.observationTime DESC")
    List<WeatherObservation> findNearestObservations(@Param("point") Point point, Pageable pageable);

    Optional<WeatherObservation> findFirstByOrderByObservationTimeDesc();

    List<WeatherObservation> findByObservationTimeAfterOrderByObservationTimeDesc(Instant after);
}
