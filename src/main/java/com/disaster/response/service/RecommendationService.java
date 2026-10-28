package com.disaster.response.service;

import com.disaster.response.domain.entity.*;
import com.disaster.response.domain.enums.*;
import com.disaster.response.dto.recommendation.RecommendationResponse;
import com.disaster.response.exception.InvalidStateTransitionException;
import com.disaster.response.exception.ResourceNotFoundException;
import com.disaster.response.integration.routing.RoutingService;
import com.disaster.response.repository.AssignmentRecommendationRepository;
import com.disaster.response.repository.IncidentAssignmentRepository;
import com.disaster.response.repository.IncidentRepository;
import com.disaster.response.repository.RegionRepository;
import com.disaster.response.repository.ResponderTeamRepository;
import com.disaster.response.repository.RiskAssessmentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

@Service
public class RecommendationService {

    private static final Logger log = LoggerFactory.getLogger(RecommendationService.class);

    private static final List<AssignmentStatus> TERMINAL_ASSIGNMENT_STATUSES =
            List.of(AssignmentStatus.RESOLVED, AssignmentStatus.CANCELLED, AssignmentStatus.REJECTED);

    private final IncidentRepository incidentRepository;
    private final ResponderTeamRepository teamRepository;
    private final AssignmentRecommendationRepository recommendationRepository;
    private final IncidentAssignmentRepository assignmentRepository;
    private final RoutingService routingService;
    private final AuditService auditService;
    private final RiskAssessmentRepository riskAssessmentRepository;
    private final RegionRepository regionRepository;

    public RecommendationService(
            IncidentRepository incidentRepository,
            ResponderTeamRepository teamRepository,
            AssignmentRecommendationRepository recommendationRepository,
            IncidentAssignmentRepository assignmentRepository,
            RoutingService routingService,
            AuditService auditService) {
        this(incidentRepository, teamRepository, recommendationRepository, assignmentRepository, routingService, auditService, null, null);
    }

    @Autowired
    public RecommendationService(
            IncidentRepository incidentRepository,
            ResponderTeamRepository teamRepository,
            AssignmentRecommendationRepository recommendationRepository,
            IncidentAssignmentRepository assignmentRepository,
            RoutingService routingService,
            AuditService auditService,
            RiskAssessmentRepository riskAssessmentRepository,
            RegionRepository regionRepository) {
        this.incidentRepository = incidentRepository;
        this.teamRepository = teamRepository;
        this.recommendationRepository = recommendationRepository;
        this.assignmentRepository = assignmentRepository;
        this.routingService = routingService;
        this.auditService = auditService;
        this.riskAssessmentRepository = riskAssessmentRepository;
        this.regionRepository = regionRepository;
    }

    /**
     * Generates explainable, ranked team recommendations for a VERIFIED incident.
     * Enforces:
     * - Rule 3: Incident must be verified before team recommendations.
     * - Rule 4: Recommendations are NOT dispatches.
     * - Checks for existing active assignments to prevent duplicate dispatches.
     */
    @Transactional
    public List<RecommendationResponse> generateRecommendations(UUID incidentId, User admin) {
        Incident incident = incidentRepository.findById(incidentId)
                .orElseThrow(() -> new ResourceNotFoundException("Incident not found with ID: " + incidentId));

        if (incident.getVerificationStatus() != VerificationStatus.VERIFIED) {
            throw new InvalidStateTransitionException(
                    "Only VERIFIED incidents are eligible for team recommendations. Current verification status: "
                            + incident.getVerificationStatus()
            );
        }

        if (assignmentRepository.existsByIncidentIdAndAssignmentStatusNotIn(incidentId, TERMINAL_ASSIGNMENT_STATUSES)) {
            throw new InvalidStateTransitionException(
                    "An active assignment already exists for this incident. Cannot generate new recommendations without reassignment."
            );
        }

        // Archive previous unapproved recommendations for this incident
        List<AssignmentRecommendation> previous = recommendationRepository
                .findByIncidentIdAndRecommendationStatus(incidentId, RecommendationStatus.PROPOSED);
        for (AssignmentRecommendation r : previous) {
            r.setRecommendationStatus(RecommendationStatus.SUPERSEDED);
            recommendationRepository.save(r);
        }

        // Find available teams
        List<ResponderTeam> availableTeams = teamRepository.findByAvailabilityStatus(AvailabilityStatus.AVAILABLE);
        if (availableTeams.isEmpty()) {
            log.warn("No AVAILABLE responder teams found for incident {}", incidentId);
            return List.of();
        }

        Set<CapabilityType> requiredCapabilities = getRequiredCapabilities(incident.getDisasterType());

        // Check for latest AI disaster risk prediction in the incident's region
        Optional<RiskAssessment> aiRiskOpt = Optional.empty();
        if (regionRepository != null && riskAssessmentRepository != null && incident.getLocation() != null) {
            List<Region> regions = regionRepository.findRegionsContainingPoint(incident.getLocation());
            if (!regions.isEmpty()) {
                aiRiskOpt = riskAssessmentRepository.findFirstByRegionIdAndDisasterTypeOrderByPredictionTimestampDesc(
                        regions.get(0).getId(), incident.getDisasterType()
                );
            }
        }

        String aiRiskContext = "";
        double riskUrgencyBonus = 0.0;
        if (aiRiskOpt.isPresent()) {
            RiskAssessment risk = aiRiskOpt.get();
            aiRiskContext = String.format(" [AI Risk Assessment: %s (%.0f%% probability, model %s)]",
                    risk.getRiskCategory(), risk.getProbability() * 100.0, risk.getModelVersion());
            if (risk.getRiskCategory() == RiskCategory.HIGH || risk.getRiskCategory() == RiskCategory.CRITICAL) {
                // In high/critical risk zones, give an additional urgency prioritization to teams with exact capability match
                riskUrgencyBonus = 15.0;
            }
        }

        // Score each team
        record TeamScore(ResponderTeam team, double score, RoutingService.RoutingResult routing,
                         boolean capabilityMatch, String reason) {}
        List<TeamScore> scoredTeams = new ArrayList<>();

        for (ResponderTeam team : availableTeams) {
            RoutingService.RoutingResult routing = routingService.calculateRoute(team.getBaseLocation(), incident.getLocation());

            boolean hasCapabilityMatch = team.getCapabilities().stream()
                    .anyMatch(c -> requiredCapabilities.contains(c.getCapability()));

            // Scoring components:
            // 1. Capability match: 40 points + AI risk urgency bonus for exact match
            double capabilityScore = hasCapabilityMatch ? (40.0 + riskUrgencyBonus) : 10.0;

            // 2. Proximity: max 40 points, decaying with distance
            double distancePenalty = Math.min(35.0, routing.distanceKm() * 0.4);
            double proximityScore = Math.max(5.0, 40.0 - distancePenalty);

            // 3. Availability: 20 points
            double availabilityScore = 20.0;

            double totalScore = capabilityScore + proximityScore + availabilityScore;

            StringBuilder reason = new StringBuilder();
            reason.append(hasCapabilityMatch ? "Matches required disaster capability. " : "General response team. ");
            reason.append(String.format("Estimated distance: %.1f km (%d min via %s). ",
                    routing.distanceKm(), routing.estimatedDurationMinutes(), routing.provider()));
            reason.append("Team is currently AVAILABLE.");
            if (!aiRiskContext.isBlank()) {
                reason.append(aiRiskContext);
            }

            scoredTeams.add(new TeamScore(team, totalScore, routing, hasCapabilityMatch, reason.toString()));
        }

        // Sort descending by score
        scoredTeams.sort((a, b) -> Double.compare(b.score(), a.score()));

        List<AssignmentRecommendation> savedRecommendations = new ArrayList<>();
        int rank = 1;
        for (TeamScore ts : scoredTeams) {
            AssignmentRecommendation rec = new AssignmentRecommendation(
                    incident,
                    ts.team(),
                    rank++,
                    ts.routing().estimatedDurationMinutes(),
                    true,
                    ts.team().getAvailabilityStatus().name(),
                    ts.reason()
            );
            savedRecommendations.add(recommendationRepository.save(rec));
        }

        auditService.record(admin, "GENERATE_RECOMMENDATIONS", "Incident", incidentId.toString(),
                null, "generated=" + savedRecommendations.size() + " recommendations");

        log.info("Generated {} ranked recommendations for incident {}", savedRecommendations.size(), incidentId);

        return savedRecommendations.stream().map(RecommendationResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<RecommendationResponse> getRecommendations(UUID incidentId) {
        return recommendationRepository.findByIncidentIdOrderByRankAsc(incidentId)
                .stream()
                .map(RecommendationResponse::from)
                .toList();
    }

    /**
     * Rejects a recommendation.
     * Enforces:
     * - Rule 6: A rejected recommendation must NEVER dispatch a team.
     */
    @Transactional
    public RecommendationResponse rejectRecommendation(UUID recommendationId, String reason, User admin) {
        AssignmentRecommendation rec = recommendationRepository.findById(recommendationId)
                .orElseThrow(() -> new ResourceNotFoundException("Recommendation not found with ID: " + recommendationId));

        if (rec.getRecommendationStatus() != RecommendationStatus.PROPOSED) {
            throw new InvalidStateTransitionException(
                    "Only PROPOSED recommendations can be rejected. Current status: " + rec.getRecommendationStatus()
            );
        }

        rec.setRecommendationStatus(RecommendationStatus.REJECTED);
        AssignmentRecommendation saved = recommendationRepository.save(rec);

        auditService.record(admin, "REJECT_RECOMMENDATION", "AssignmentRecommendation",
                saved.getId().toString(), "status=PROPOSED", "status=REJECTED, reason=" + reason);

        log.info("Recommendation {} rejected by admin {}: {}", saved.getId(), admin.getEmail(), reason);
        return RecommendationResponse.from(saved);
    }

    private Set<CapabilityType> getRequiredCapabilities(DisasterType disasterType) {
        if (disasterType == null) {
            return Set.of(CapabilityType.FIRST_AID);
        }
        return switch (disasterType) {
            case FLOOD, TSUNAMI -> Set.of(CapabilityType.WATER_RESCUE, CapabilityType.BOAT_OPERATIONS, CapabilityType.EVACUATION_LOGISTICS);
            case EARTHQUAKE -> Set.of(CapabilityType.COLLAPSED_STRUCTURE, CapabilityType.CANINE_SEARCH, CapabilityType.FIRST_AID);
            case WILDFIRE -> Set.of(CapabilityType.WILDFIRE_SUPPRESSION, CapabilityType.EVACUATION_LOGISTICS);
            case LANDSLIDE -> Set.of(CapabilityType.COLLAPSED_STRUCTURE, CapabilityType.HIGH_ANGLE_RESCUE);
            case HAZMAT_SPILL -> Set.of(CapabilityType.HAZMAT_CONTAINMENT, CapabilityType.ADVANCED_LIFE_SUPPORT);
            default -> Set.of(CapabilityType.FIRST_AID, CapabilityType.EVACUATION_LOGISTICS);
        };
    }
}
