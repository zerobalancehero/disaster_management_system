package com.disaster.response.domain.entity;

import com.disaster.response.domain.enums.RunStatus;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "data_source_runs")
public class DataSourceRun {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String provider;

    @Column(name = "job_type", nullable = false)
    private String jobType;

    @Column(name = "start_time", nullable = false)
    private Instant startTime = Instant.now();

    @Column(name = "end_time")
    private Instant endTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RunStatus status = RunStatus.RUNNING;

    @Column(name = "records_processed", nullable = false)
    private int recordsProcessed = 0;

    @Column(name = "error_summary", columnDefinition = "TEXT")
    private String errorSummary;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public DataSourceRun() {
    }

    public DataSourceRun(String provider, String jobType) {
        this.provider = provider;
        this.jobType = jobType;
        this.startTime = Instant.now();
        this.status = RunStatus.RUNNING;
        this.recordsProcessed = 0;
        this.createdAt = Instant.now();
    }

    public void complete(int recordsProcessed) {
        this.endTime = Instant.now();
        this.status = RunStatus.SUCCESS;
        this.recordsProcessed = recordsProcessed;
    }

    public void fail(String errorSummary) {
        this.endTime = Instant.now();
        this.status = RunStatus.FAILURE;
        this.errorSummary = errorSummary;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getJobType() {
        return jobType;
    }

    public void setJobType(String jobType) {
        this.jobType = jobType;
    }

    public Instant getStartTime() {
        return startTime;
    }

    public void setStartTime(Instant startTime) {
        this.startTime = startTime;
    }

    public Instant getEndTime() {
        return endTime;
    }

    public void setEndTime(Instant endTime) {
        this.endTime = endTime;
    }

    public RunStatus getStatus() {
        return status;
    }

    public void setStatus(RunStatus status) {
        this.status = status;
    }

    public int getRecordsProcessed() {
        return recordsProcessed;
    }

    public void setRecordsProcessed(int recordsProcessed) {
        this.recordsProcessed = recordsProcessed;
    }

    public String getErrorSummary() {
        return errorSummary;
    }

    public void setErrorSummary(String errorSummary) {
        this.errorSummary = errorSummary;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
