package com.disaster.response;

import com.disaster.response.domain.entity.*;
import com.disaster.response.domain.enums.*;
import com.disaster.response.dto.assignment.AssignmentResponse;
import com.disaster.response.dto.assignment.UpdateAssignmentStatusRequest;
import com.disaster.response.dto.recommendation.RecommendationResponse;
import com.disaster.response.exception.IdempotencyConflictException;
import com.disaster.response.exception.InvalidStateTransitionException;
import com.disaster.response.gis.GisUtil;
import com.disaster.response.integration.routing.HaversineRoutingService;
import com.disaster.response.repository.*;
import com.disaster.response.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmergencyDispatchEngineTest {

    @Mock private IncidentRepository incidentRepository;
    @Mock private ResponderTeamRepository teamRepository;
    @Mock private AssignmentRecommendationRepository recommendationRepository;
    @Mock private IncidentAssignmentRepository assignmentRepository;
    @Mock private ResponseUpdateRepository responseUpdateRepository;
    @Mock private TeamMemberRepository teamMemberRepository;
    @Mock private NotificationService notificationService;
    @Mock private AuditService auditService;
    @Mock private TeamService teamService;
    @Mock private RiskAssessmentRepository riskAssessmentRepository;
    @Mock private RegionRepository regionRepository;

    private RecommendationService recommendationService;
    private AssignmentService assignmentService;

    private User admin;
    private User responder;
    private User citizen;
    private ResponderTeam teamAlpha;
    private Incident verifiedIncident;
    private Incident unverifiedIncident;

    @BeforeEach
    void setUp() {
        recommendationService = new RecommendationService(
                incidentRepository,
                teamRepository,
                recommendationRepository,
                assignmentRepository,
                new HaversineRoutingService(),
                auditService,
                riskAssessmentRepository,
                regionRepository
        );

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

        admin = new User("Admin", "admin@disaster.local", "hash", null, UserRole.ADMIN);
        admin.setId(UUID.randomUUID());

        responder = new User("Responder", "responder@disaster.local", "hash", null, UserRole.RESPONDER);
        responder.setId(UUID.randomUUID());

        citizen = new User("Citizen", "citizen@disaster.local", "hash", null, UserRole.USER);
        citizen.setId(UUID.randomUUID());

        teamAlpha = new ResponderTeam("Alpha Rescue", TeamType.SEARCH_AND_RESCUE, AvailabilityStatus.AVAILABLE,
                GisUtil.createPoint(13.08, 80.27), "alpha@rescue.gov");
        teamAlpha.setId(UUID.randomUUID());

        verifiedIncident = new Incident(citizen, "Verified Flood", "Water entered homes",
                DisasterType.FLOOD, IncidentSeverity.HIGH, GisUtil.createPoint(13.06, 80.24));
        verifiedIncident.setId(UUID.randomUUID());
        verifiedIncident.setVerificationStatus(VerificationStatus.VERIFIED);

        unverifiedIncident = new Incident(citizen, "Pending Earthquake", "Wall crack",
                DisasterType.EARTHQUAKE, IncidentSeverity.MEDIUM, GisUtil.createPoint(13.04, 80.25));
        unverifiedIncident.setId(UUID.randomUUID());
        unverifiedIncident.setVerificationStatus(VerificationStatus.PENDING);
    }

    @Test
    @DisplayName("Rule 3: Unverified incidents CANNOT receive recommendations")
    void testUnverifiedIncidentsCannotReceiveRecommendations() {
        when(incidentRepository.findById(unverifiedIncident.getId())).thenReturn(Optional.of(unverifiedIncident));

        InvalidStateTransitionException ex = assertThrows(InvalidStateTransitionException.class, () ->
                recommendationService.generateRecommendations(unverifiedIncident.getId(), admin)
        );
        assertTrue(ex.getMessage().contains("Only VERIFIED incidents are eligible"));
    }

    @Test
    @DisplayName("Rule 4: Recommendation creation does NOT dispatch a team")
    void testRecommendationCreationDoesNotDispatchTeam() {
        when(incidentRepository.findById(verifiedIncident.getId())).thenReturn(Optional.of(verifiedIncident));
        when(assignmentRepository.existsByIncidentIdAndAssignmentStatusNotIn(any(), any())).thenReturn(false);
        when(teamRepository.findByAvailabilityStatus(AvailabilityStatus.AVAILABLE)).thenReturn(List.of(teamAlpha));
        when(recommendationRepository.save(any(AssignmentRecommendation.class))).thenAnswer(i -> {
            AssignmentRecommendation r = i.getArgument(0);
            r.setId(UUID.randomUUID());
            return r;
        });

        List<RecommendationResponse> recommendations = recommendationService.generateRecommendations(verifiedIncident.getId(), admin);

        assertFalse(recommendations.isEmpty());
        RecommendationResponse first = recommendations.get(0);
        assertEquals(RecommendationStatus.PROPOSED, first.recommendationStatus());

        // Team must STILL be AVAILABLE
        assertEquals(AvailabilityStatus.AVAILABLE, teamAlpha.getAvailabilityStatus());

        // No IncidentAssignment should be saved
        verify(assignmentRepository, never()).save(any(IncidentAssignment.class));
    }

    @Test
    @DisplayName("Rule 5 & 11: Dispatch requires explicit admin approval; creates assignment and prevents double approval")
    void testDispatchRequiresApprovalAndPreventsDoubleApproval() {
        UUID recId = UUID.randomUUID();
        AssignmentRecommendation rec = new AssignmentRecommendation(
                verifiedIncident, teamAlpha, 1, 25, true, "AVAILABLE", "Top ranked match"
        );
        rec.setId(recId);
        rec.setRecommendationStatus(RecommendationStatus.PROPOSED);

        when(recommendationRepository.findById(recId)).thenReturn(Optional.of(rec));
        when(assignmentRepository.existsByIncidentIdAndAssignmentStatusNotIn(any(), any())).thenReturn(false);
        when(assignmentRepository.save(any(IncidentAssignment.class))).thenAnswer(i -> {
            IncidentAssignment a = i.getArgument(0);
            a.setId(UUID.randomUUID());
            return a;
        });

        // 1. First approval succeeds
        AssignmentResponse assignment = assignmentService.approveRecommendationAndDispatch(recId, admin);
        assertNotNull(assignment);
        assertEquals(AssignmentStatus.APPROVED, assignment.assignmentStatus());
        assertEquals(AvailabilityStatus.DISPATCHED, teamAlpha.getAvailabilityStatus());
        assertEquals(RecommendationStatus.APPROVED, rec.getRecommendationStatus());

        // Verify audit record was created
        verify(auditService, times(1)).record(eq(admin), eq("APPROVE_DISPATCH"), eq("IncidentAssignment"),
                any(), any(), any());

        // 2. Second approval attempt on already approved recommendation must throw IdempotencyConflictException
        assertThrows(IdempotencyConflictException.class, () ->
                assignmentService.approveRecommendationAndDispatch(recId, admin)
        );
    }

    @Test
    @DisplayName("Rule 6: Rejected recommendations CANNOT be dispatched")
    void testRejectedRecommendationsCannotBeDispatched() {
        UUID recId = UUID.randomUUID();
        AssignmentRecommendation rec = new AssignmentRecommendation(
                verifiedIncident, teamAlpha, 1, 25, true, "AVAILABLE", "Top ranked match"
        );
        rec.setId(recId);
        rec.setRecommendationStatus(RecommendationStatus.PROPOSED);

        when(recommendationRepository.findById(recId)).thenReturn(Optional.of(rec));
        when(recommendationRepository.save(any(AssignmentRecommendation.class))).thenAnswer(i -> i.getArgument(0));

        // Admin rejects recommendation
        RecommendationResponse rejected = recommendationService.rejectRecommendation(recId, "Team needed elsewhere", admin);
        assertEquals(RecommendationStatus.REJECTED, rejected.recommendationStatus());

        // Trying to dispatch the rejected recommendation MUST fail
        assertThrows(IdempotencyConflictException.class, () ->
                assignmentService.approveRecommendationAndDispatch(recId, admin)
        );
    }

    @Test
    @DisplayName("Rule 9: Assignment status changes follow explicit state machine; invalid transitions are rejected")
    void testInvalidAssignmentStatusTransitionsAreRejected() {
        UUID assignmentId = UUID.randomUUID();
        IncidentAssignment assignment = new IncidentAssignment(
                verifiedIncident, teamAlpha, admin, AssignmentStatus.APPROVED
        );
        assignment.setId(assignmentId);

        when(teamService.getTeamIdsForUser(responder.getId())).thenReturn(List.of(teamAlpha.getId()));
        when(assignmentRepository.findById(assignmentId)).thenReturn(Optional.of(assignment));

        // Invalid: Skipping ACCEPTED and EN_ROUTE directly to ON_SCENE is disallowed from APPROVED
        UpdateAssignmentStatusRequest invalidRequest = new UpdateAssignmentStatusRequest(
                AssignmentStatus.ON_SCENE, "Skipping steps", null
        );

        assertThrows(InvalidStateTransitionException.class, () ->
                assignmentService.updateAssignmentStatus(assignmentId, invalidRequest, responder)
        );

        // Valid: APPROVED -> ACCEPTED is allowed
        when(assignmentRepository.save(any(IncidentAssignment.class))).thenAnswer(i -> i.getArgument(0));
        UpdateAssignmentStatusRequest validRequest = new UpdateAssignmentStatusRequest(
                AssignmentStatus.ACCEPTED, "Team accepted dispatch", null
        );

        AssignmentResponse response = assignmentService.updateAssignmentStatus(assignmentId, validRequest, responder);
        assertEquals(AssignmentStatus.ACCEPTED, response.assignmentStatus());
    }

    @Test
    @DisplayName("AI-Assisted Dispatch: High AI risk prediction enriches recommendation reason and urgency")
    void testAiRiskAssessmentEnrichesRecommendation() {
        Region coastalRegion = new Region("Coastal Zone", RegionType.MUNICIPALITY, null, GisUtil.createPoint(13.06, 80.24));
        coastalRegion.setId(UUID.randomUUID());

        RiskAssessment highRisk = new RiskAssessment(
                coastalRegion, DisasterType.FLOOD, 0.88, RiskCategory.HIGH, PredictionHorizon.NEXT_24H,
                "PROD-ONNX-v1.0", "{}", "{}"
        );

        when(incidentRepository.findById(verifiedIncident.getId())).thenReturn(Optional.of(verifiedIncident));
        when(assignmentRepository.existsByIncidentIdAndAssignmentStatusNotIn(any(), any())).thenReturn(false);
        when(teamRepository.findByAvailabilityStatus(AvailabilityStatus.AVAILABLE)).thenReturn(List.of(teamAlpha));
        when(regionRepository.findRegionsContainingPoint(any())).thenReturn(List.of(coastalRegion));
        when(riskAssessmentRepository.findFirstByRegionIdAndDisasterTypeOrderByPredictionTimestampDesc(eq(coastalRegion.getId()), eq(DisasterType.FLOOD)))
                .thenReturn(Optional.of(highRisk));
        when(recommendationRepository.save(any(AssignmentRecommendation.class))).thenAnswer(i -> {
            AssignmentRecommendation r = i.getArgument(0);
            r.setId(UUID.randomUUID());
            return r;
        });

        List<RecommendationResponse> recs = recommendationService.generateRecommendations(verifiedIncident.getId(), admin);
        assertNotNull(recs);
        assertEquals(1, recs.size());
        assertTrue(recs.get(0).recommendationReason().contains("AI Risk Assessment: HIGH"));
        assertTrue(recs.get(0).recommendationReason().contains("88% probability"));
        assertTrue(recs.get(0).recommendationReason().contains("model PROD-ONNX-v1.0"));
    }
}
