package com.disaster.response;

import com.disaster.response.domain.entity.Incident;
import com.disaster.response.domain.entity.IncidentAssignment;
import com.disaster.response.domain.entity.ResponderTeam;
import com.disaster.response.domain.entity.User;
import com.disaster.response.domain.enums.*;
import com.disaster.response.dto.assignment.AssignmentResponse;
import com.disaster.response.dto.assignment.UpdateAssignmentStatusRequest;
import com.disaster.response.exception.UnauthorizedActionException;
import com.disaster.response.gis.GisUtil;
import com.disaster.response.repository.*;
import com.disaster.response.service.AssignmentService;
import com.disaster.response.service.AuditService;
import com.disaster.response.service.NotificationService;
import com.disaster.response.service.TeamService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoleBasedAccessSecurityTest {

    @Mock private IncidentAssignmentRepository assignmentRepository;
    @Mock private AssignmentRecommendationRepository recommendationRepository;
    @Mock private IncidentRepository incidentRepository;
    @Mock private ResponderTeamRepository teamRepository;
    @Mock private ResponseUpdateRepository responseUpdateRepository;
    @Mock private TeamMemberRepository teamMemberRepository;
    @Mock private NotificationService notificationService;
    @Mock private AuditService auditService;
    @Mock private TeamService teamService;

    private AssignmentService assignmentService;

    @BeforeEach
    void setUp() {
        assignmentService = new AssignmentService(
                assignmentRepository,
                recommendationRepository,
                incidentRepository,
                teamRepository,
                responseUpdateRepository,
                teamMemberRepository,
                notificationService,
                auditService,
                teamService
        );
    }

    @Test
    @DisplayName("Security Rule 7: Responders CANNOT read another team's assignment")
    void testResponderCannotReadAnotherTeamsAssignment() {
        UUID responderId = UUID.randomUUID();
        User responder = new User("Responder 1", "resp1@disaster.local", "hash", null, UserRole.RESPONDER);
        responder.setId(responderId);

        UUID authorizedTeamId = UUID.randomUUID();
        UUID otherTeamId = UUID.randomUUID();

        // Responder is authorized ONLY for authorizedTeamId
        when(teamService.getTeamIdsForUser(responderId)).thenReturn(List.of(authorizedTeamId));

        // Create assignment belonging to otherTeamId
        UUID assignmentId = UUID.randomUUID();
        ResponderTeam otherTeam = new ResponderTeam("Bravo Team", TeamType.SEARCH_AND_RESCUE, AvailabilityStatus.AVAILABLE,
                GisUtil.createPoint(13.0, 80.0), "contact");
        otherTeam.setId(otherTeamId);

        Incident incident = new Incident(responder, "Test Incident", "Desc", DisasterType.FLOOD, IncidentSeverity.HIGH, GisUtil.createPoint(13.0, 80.0));
        IncidentAssignment assignment = new IncidentAssignment(incident, otherTeam, responder, AssignmentStatus.APPROVED);
        assignment.setId(assignmentId);

        when(assignmentRepository.findById(assignmentId)).thenReturn(Optional.of(assignment));

        // Attempting to read other team's assignment MUST throw UnauthorizedActionException
        UnauthorizedActionException ex = assertThrows(UnauthorizedActionException.class, () ->
                assignmentService.getResponderAssignmentById(assignmentId, responder)
        );
        assertTrue(ex.getMessage().contains("not an authorized member"));
    }

    @Test
    @DisplayName("Security Rule 7: Responders CAN read their own authorized team's assignment")
    void testResponderCanReadOwnTeamAssignment() {
        UUID responderId = UUID.randomUUID();
        User responder = new User("Responder 1", "resp1@disaster.local", "hash", null, UserRole.RESPONDER);
        responder.setId(responderId);

        UUID myTeamId = UUID.randomUUID();
        when(teamService.getTeamIdsForUser(responderId)).thenReturn(List.of(myTeamId));

        UUID assignmentId = UUID.randomUUID();
        ResponderTeam myTeam = new ResponderTeam("Alpha Team", TeamType.SEARCH_AND_RESCUE, AvailabilityStatus.AVAILABLE,
                GisUtil.createPoint(13.0, 80.0), "contact");
        myTeam.setId(myTeamId);

        Incident incident = new Incident(responder, "Test Incident", "Desc", DisasterType.FLOOD, IncidentSeverity.HIGH, GisUtil.createPoint(13.0, 80.0));
        IncidentAssignment assignment = new IncidentAssignment(incident, myTeam, responder, AssignmentStatus.APPROVED);
        assignment.setId(assignmentId);

        when(assignmentRepository.findById(assignmentId)).thenReturn(Optional.of(assignment));

        AssignmentResponse response = assignmentService.getResponderAssignmentById(assignmentId, responder);
        assertNotNull(response);
        assertEquals(assignmentId, response.id());
        assertEquals(myTeamId, response.teamId());
    }

    @Test
    @DisplayName("Security Rule 7: Responders without team membership get empty assignments list")
    void testResponderWithoutTeamMembershipGetsEmptyAssignments() {
        UUID responderId = UUID.randomUUID();
        User responder = new User("Unassigned Responder", "unassigned@disaster.local", "hash", null, UserRole.RESPONDER);
        responder.setId(responderId);

        when(teamService.getTeamIdsForUser(responderId)).thenReturn(List.of());

        Page<AssignmentResponse> results = assignmentService.getResponderAssignments(responder, PageRequest.of(0, 10));
        assertTrue(results.isEmpty());
    }
}
