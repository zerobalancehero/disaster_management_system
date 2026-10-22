package com.disaster.response.repository;

import com.disaster.response.domain.entity.ModelRegistry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ModelRegistryRepository extends JpaRepository<ModelRegistry, UUID> {
    Optional<ModelRegistry> findByModelNameAndActiveTrue(String modelName);
    Optional<ModelRegistry> findByActiveTrue();
    Optional<ModelRegistry> findByModelNameAndVersion(String modelName, String version);
    List<ModelRegistry> findAllByOrderByCreatedAtDesc();
}
