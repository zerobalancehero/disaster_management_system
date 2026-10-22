package com.disaster.response.repository;

import com.disaster.response.domain.entity.TerrainFeature;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface TerrainFeatureRepository extends JpaRepository<TerrainFeature, UUID> {
    Optional<TerrainFeature> findFirstByRegionIdOrderByProcessingTimeDesc(UUID regionId);
}
