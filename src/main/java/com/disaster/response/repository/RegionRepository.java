package com.disaster.response.repository;

import com.disaster.response.domain.entity.Region;
import com.disaster.response.domain.enums.RegionType;
import org.locationtech.jts.geom.Point;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RegionRepository extends JpaRepository<Region, UUID> {
    List<Region> findByRegionType(RegionType regionType);
    Page<Region> findByRegionType(RegionType regionType, Pageable pageable);

    @Query("SELECT r FROM Region r WHERE ST_Contains(r.boundary, :point) = true")
    List<Region> findRegionsContainingPoint(@Param("point") Point point);
}
