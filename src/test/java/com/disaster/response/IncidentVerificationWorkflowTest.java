package com.disaster.response;

import com.disaster.response.domain.entity.Incident;
import com.disaster.response.domain.entity.User;
import com.disaster.response.domain.enums.DisasterType;
import com.disaster.response.domain.enums.IncidentSeverity;
import com.disaster.response.domain.enums.UserRole;
import com.disaster.response.domain.enums.VerificationStatus;
import com.disaster.response.dto.incident.IncidentResponse;
import com.disaster.response.dto.incident.RejectIncidentRequest;
import com.disaster.response.dto.incident.VerifyIncidentRequest;
import com.disaster.response.exception.InvalidStateTransitionException;
import com.disaster.response.gis.GisUtil;
import com.disaster.response.repository.IncidentRepository;
import com.disaster.response.service.AuditService;
import com.disaster.response.service.IncidentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IncidentVerificationWorkflowTest {

    @Mock private IncidentRepository incidentRepository;
    @Mock private AuditService auditService;

    private IncidentService incidentService;
    private User admin;
    private User citizen;

    @BeforeEach
    void setUp() {
        incidentService = new IncidentService(incidentRepository, auditService);

        admin = new User("Admin", "admin@disaster.local", "hash", null, UserRole.ADMIN);
        admin.setId(UUID.randomUUID());

        citizen = new User("Citizen", "citizen@disaster.local", "hash", null, UserRole.USER);
        citizen.setId(UUID.randomUUID());
    }

    @Test
    @DisplayName("Admin Incident Verification: Transitions PENDING -> VERIFIED and records audit log")
    void testVerifyIncidentSuccess() {
        UUID incidentId = UUID.randomUUID();
        Incident incident = new Incident(citizen, "Flood Report", "Water entering homes",
                DisasterType.FLOOD, IncidentSeverity.MEDIUM, GisUtil.createPoint(13.0, 80.0));
        incident.setId(incidentId);

        when(incidentRepository.findById(incidentId)).thenReturn(Optional.of(incident));
        when(incidentRepository.save(any(Incident.class))).thenAnswer(i -> i.getArgument(0));

        VerifyIncidentRequest request = new VerifyIncidentRequest(IncidentSeverity.HIGH, "Confirmed with field video");
        IncidentResponse response = incidentService.verifyIncident(incidentId, request, admin);

        assertEquals(VerificationStatus.VERIFIED, response.verificationStatus());
        assertEquals(IncidentSeverity.HIGH, response.severity());
        assertEquals(admin.getId(), response.verifierId());
        assertNotNull(response.verificationTime());

        verify(auditService, times(1)).record(eq(admin), eq("VERIFY_INCIDENT"), eq("Incident"),
                eq(incidentId.toString()), any(), any());
    }

    @Test
    @DisplayName("Admin Incident Rejection: Requires reason, transitions PENDING -> REJECTED and records audit log")
    void testRejectIncidentSuccess() {
        UUID incidentId = UUID.randomUUID();
        Incident incident = new Incident(citizen, "False Alarm", "Drain overflow",
                DisasterType.FLOOD, IncidentSeverity.LOW, GisUtil.createPoint(13.0, 80.0));
        incident.setId(incidentId);

        when(incidentRepository.findById(incidentId)).thenReturn(Optional.of(incident));
        when(incidentRepository.save(any(Incident.class))).thenAnswer(i -> i.getArgument(0));

        RejectIncidentRequest request = new RejectIncidentRequest("Inspected by local patrol; drain is functioning normally.");
        IncidentResponse response = incidentService.rejectIncident(incidentId, request, admin);

        assertEquals(VerificationStatus.REJECTED, response.verificationStatus());
        assertEquals("Inspected by local patrol; drain is functioning normally.", response.rejectionReason());
        assertEquals(admin.getId(), response.verifierId());

        verify(auditService, times(1)).record(eq(admin), eq("REJECT_INCIDENT"), eq("Incident"),
                eq(incidentId.toString()), any(), any());
    }

    @Test
    @DisplayName("Verification State Machine: Cannot re-verify an already VERIFIED incident")
    void testCannotReverifyAlreadyVerifiedIncident() {
        UUID incidentId = UUID.randomUUID();
        Incident incident = new Incident(citizen, "Flood Report", "Desc",
                DisasterType.FLOOD, IncidentSeverity.HIGH, GisUtil.createPoint(13.0, 80.0));
        incident.setId(incidentId);
        incident.setVerificationStatus(VerificationStatus.VERIFIED);

        when(incidentRepository.findById(incidentId)).thenReturn(Optional.of(incident));

        assertThrows(InvalidStateTransitionException.class, () ->
                incidentService.verifyIncident(incidentId, new VerifyIncidentRequest(null, null), admin)
        );
    }
}
