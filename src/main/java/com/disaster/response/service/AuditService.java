package com.disaster.response.service;

import com.disaster.response.domain.entity.AuditLog;
import com.disaster.response.domain.entity.User;
import com.disaster.response.dto.report.AuditLogResponse;
import com.disaster.response.repository.AuditLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditService.class);

    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void record(User actor, String action, String entityType, String entityId, String oldValues, String newValues) {
        AuditLog auditLog = new AuditLog(actor, action, entityType, entityId, oldValues, newValues);
        auditLogRepository.save(auditLog);
        log.info("AUDIT: Actor='{}', Action='{}', Entity='{}:{}'",
                actor != null ? actor.getEmail() : "SYSTEM", action, entityType, entityId);
    }

    @Transactional(readOnly = true)
    public Page<AuditLogResponse> getAuditLogs(Pageable pageable) {
        return auditLogRepository.findAllByOrderByTimestampDesc(pageable)
                .map(AuditLogResponse::from);
    }
}
