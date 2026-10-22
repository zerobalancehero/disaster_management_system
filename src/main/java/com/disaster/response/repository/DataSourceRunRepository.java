package com.disaster.response.repository;

import com.disaster.response.domain.entity.DataSourceRun;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DataSourceRunRepository extends JpaRepository<DataSourceRun, UUID> {
    Optional<DataSourceRun> findFirstByProviderOrderByStartTimeDesc(String provider);
    List<DataSourceRun> findTop20ByOrderByStartTimeDesc();
    Page<DataSourceRun> findAllByOrderByStartTimeDesc(Pageable pageable);
}
