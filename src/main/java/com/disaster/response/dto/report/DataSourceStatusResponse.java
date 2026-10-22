package com.disaster.response.dto.report;

import com.disaster.response.domain.entity.DataSourceRun;
import com.disaster.response.domain.enums.RunStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record DataSourceStatusResponse(
    List<ProviderStatusDto> providers
) {
    public record ProviderStatusDto(
        UUID lastRunId,
        String provider,
        String jobType,
        Instant lastRunTime,
        RunStatus status,
        int recordsProcessed,
        String errorSummary,
        boolean isFresh
    ) {
        public static ProviderStatusDto from(DataSourceRun run) {
            if (run == null) {
                return null;
            }
            boolean fresh = run.getStatus() == RunStatus.SUCCESS &&
                    run.getStartTime() != null &&
                    java.time.Duration.between(run.getStartTime(), Instant.now()).toHours() < 6;
            return new ProviderStatusDto(
                run.getId(),
                run.getProvider(),
                run.getJobType(),
                run.getStartTime(),
                run.getStatus(),
                run.getRecordsProcessed(),
                run.getErrorSummary(),
                fresh
            );
        }
    }
}
