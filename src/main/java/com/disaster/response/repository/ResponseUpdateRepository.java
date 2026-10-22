package com.disaster.response.repository;

import com.disaster.response.domain.entity.ResponseUpdate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ResponseUpdateRepository extends JpaRepository<ResponseUpdate, UUID> {
    List<ResponseUpdate> findByAssignmentIdOrderByTimestampAsc(UUID assignmentId);
}
