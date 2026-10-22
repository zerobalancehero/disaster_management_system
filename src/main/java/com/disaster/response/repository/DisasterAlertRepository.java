package com.disaster.response.repository;

import com.disaster.response.domain.entity.DisasterAlert;
import com.disaster.response.domain.enums.AlertStatus;
import com.disaster.response.domain.enums.DisasterType;
import org.locationtech.jts.geom.Geometry;
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
public interface DisasterAlertRepository extends JpaRepository<DisasterAlert, UUID> {

    boolean existsBySourceAndExternalSourceId(String source, String externalSourceId);

    Optional<DisasterAlert> findBySourceAndExternalSourceId(String source, String externalSourceId);

    List<DisasterAlert> findByStatusOrderByIssueTimeDesc(AlertStatus status);

    Page<DisasterAlert> findByStatusOrderByIssueTimeDesc(AlertStatus status, Pageable pageable);

    @Query("SELECT a FROM DisasterAlert a WHERE a.status = 'ACTIVE' AND (a.expiryTime IS NULL OR a.expiryTime > :now) ORDER BY a.issueTime DESC")
    List<DisasterAlert> findActiveAlerts(@Param("now") Instant now);

    @Query("SELECT a FROM DisasterAlert a WHERE a.status = 'ACTIVE' AND (a.expiryTime IS NULL OR a.expiryTime > :now) ORDER BY a.issueTime DESC")
    Page<DisasterAlert> findActiveAlerts(@Param("now") Instant now, Pageable pageable);

    @Query("SELECT a FROM DisasterAlert a WHERE ST_Intersects(a.affectedArea, :geometry) = true AND a.status = 'ACTIVE'")
    List<DisasterAlert> findAlertsIntersecting(@Param("geometry") Geometry geometry);

    @Query("SELECT a FROM DisasterAlert a WHERE ST_Contains(a.affectedArea, :point) = true AND a.status = 'ACTIVE'")
    List<DisasterAlert> findAlertsContainingPoint(@Param("point") Point point);

    List<DisasterAlert> findByDisasterType(DisasterType disasterType);
}
