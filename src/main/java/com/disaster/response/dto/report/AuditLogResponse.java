package com.disaster.response.dto.report;

import com.disaster.response.domain.entity.AuditLog;
import java.time.Instant;
import java.util.UUID;

public record AuditLogResponse(
    UUID id,
    UUID actorId,
    String actorName,
    String action,
    String entityType,
    String entityId,
    String oldValues,
    String newValues,
    Instant timestamp
) {
    public static AuditLogResponse from(AuditLog log) {
        if (log == null) {
            return null;
        }
        return new AuditLogResponse(
            log.getId(),
            log.getActor() != null ? log.getActor().getId() : null,
            log.getActor() != null ? log.getActor().getName() : "SYSTEM",
            log.getAction(),
            log.getEntityType(),
            log.getEntityId(),
            log.getOldValues(),
            log.getNewValues(),
            log.getTimestamp()
        );
    }
}
