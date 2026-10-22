package com.disaster.response.service;

import com.disaster.response.domain.entity.Incident;
import com.disaster.response.domain.entity.User;
import com.disaster.response.domain.enums.IncidentSeverity;
import com.disaster.response.domain.enums.VerificationStatus;
import com.disaster.response.dto.incident.IncidentReportRequest;
import com.disaster.response.dto.incident.IncidentResponse;
import com.disaster.response.dto.incident.RejectIncidentRequest;
import com.disaster.response.dto.incident.VerifyIncidentRequest;
import com.disaster.response.exception.InvalidStateTransitionException;
import com.disaster.response.exception.ResourceNotFoundException;
import com.disaster.response.gis.GisUtil;
import com.disaster.response.repository.IncidentRepository;
import org.locationtech.jts.geom.Point;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class IncidentService {

    private static final Logger log = LoggerFactory.getLogger(IncidentService.class);

    private final IncidentRepository incidentRepository;
    private final AuditService auditService;

    public IncidentService(IncidentRepository incidentRepository, AuditService auditService) {
        this.incidentRepository = incidentRepository;
        this.auditService = auditService;
    }

    @Transactional
    public IncidentResponse reportIncident(IncidentReportRequest request, User reporter) {
        Point loc = GisUtil.createPoint(request.location().latitude(), request.location().longitude());
        Incident incident = new Incident(
                reporter,
                request.title(),
                request.description(),
                request.disasterType(),
                request.severity(),
                loc
        );

        Incident saved = incidentRepository.save(incident);
        log.info("Incident reported: ID={}, Title='{}', DisasterType={}, Reporter={}",
                saved.getId(), saved.getTitle(), saved.getDisasterType(), reporter.getEmail());

        return IncidentResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public Page<IncidentResponse> getMyReports(UUID reporterId, Pageable pageable) {
        return incidentRepository.findByReporterIdOrderByReportedTimeDesc(reporterId, pageable)
                .map(IncidentResponse::from);
    }

    @Transactional(readOnly = true)
    public IncidentResponse getMyReportById(UUID incidentId, UUID reporterId) {
        Incident incident = incidentRepository.findByIdAndReporterId(incidentId, reporterId)
                .orElseThrow(() -> new ResourceNotFoundException("Incident report not found with ID: " + incidentId));
        return IncidentResponse.from(incident);
    }

    @Transactional(readOnly = true)
    public Page<IncidentResponse> getAllIncidents(VerificationStatus status, Pageable pageable) {
        if (status != null) {
            return incidentRepository.findByVerificationStatusOrderByReportedTimeDesc(status, pageable)
                    .map(IncidentResponse::from);
        }
        return incidentRepository.findAll(pageable).map(IncidentResponse::from);
    }

    @Transactional(readOnly = true)
    public Page<IncidentResponse> getPendingIncidents(Pageable pageable) {
        return incidentRepository.findByVerificationStatusOrderByReportedTimeDesc(VerificationStatus.PENDING, pageable)
                .map(IncidentResponse::from);
    }

    @Transactional(readOnly = true)
    public IncidentResponse getIncidentById(UUID incidentId) {
        Incident incident = incidentRepository.findById(incidentId)
                .orElseThrow(() -> new ResourceNotFoundException("Incident not found with ID: " + incidentId));
        return IncidentResponse.from(incident);
    }

    @Transactional
    public IncidentResponse verifyIncident(UUID incidentId, VerifyIncidentRequest request, User admin) {
        Incident incident = incidentRepository.findById(incidentId)
                .orElseThrow(() -> new ResourceNotFoundException("Incident not found with ID: " + incidentId));

        if (incident.getVerificationStatus() != VerificationStatus.PENDING) {
            throw new InvalidStateTransitionException(
                    "Only PENDING incidents can be verified. Current status: " + incident.getVerificationStatus()
            );
        }

        String oldValues = "status=" + incident.getVerificationStatus();
        incident.setVerificationStatus(VerificationStatus.VERIFIED);
        incident.setVerifier(admin);
        incident.setVerificationTime(Instant.now());

        if (request != null && request.severityOverride() != null) {
            incident.setSeverity(request.severityOverride());
        }

        Incident saved = incidentRepository.save(incident);
        String newValues = "status=" + saved.getVerificationStatus() + ", severity=" + saved.getSeverity();
        auditService.record(admin, "VERIFY_INCIDENT", "Incident", saved.getId().toString(), oldValues, newValues);

        log.info("Incident {} verified by admin {}", saved.getId(), admin.getEmail());
        return IncidentResponse.from(saved);
    }

    @Transactional
    public IncidentResponse rejectIncident(UUID incidentId, RejectIncidentRequest request, User admin) {
        Incident incident = incidentRepository.findById(incidentId)
                .orElseThrow(() -> new ResourceNotFoundException("Incident not found with ID: " + incidentId));

        if (incident.getVerificationStatus() != VerificationStatus.PENDING) {
            throw new InvalidStateTransitionException(
                    "Only PENDING incidents can be rejected. Current status: " + incident.getVerificationStatus()
            );
        }

        String oldValues = "status=" + incident.getVerificationStatus();
        incident.setVerificationStatus(VerificationStatus.REJECTED);
        incident.setVerifier(admin);
        incident.setVerificationTime(Instant.now());
        incident.setRejectionReason(request.rejectionReason());

        Incident saved = incidentRepository.save(incident);
        String newValues = "status=" + saved.getVerificationStatus() + ", reason=" + saved.getRejectionReason();
        auditService.record(admin, "REJECT_INCIDENT", "Incident", saved.getId().toString(), oldValues, newValues);

        log.info("Incident {} rejected by admin {}: {}", saved.getId(), admin.getEmail(), request.rejectionReason());
        return IncidentResponse.from(saved);
    }
}
