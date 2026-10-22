package com.disaster.response.service;

import com.disaster.response.domain.entity.DataSourceRun;
import com.disaster.response.domain.entity.IncidentAssignment;
import com.disaster.response.domain.enums.AssignmentStatus;
import com.disaster.response.domain.enums.AvailabilityStatus;
import com.disaster.response.domain.enums.IncidentSeverity;
import com.disaster.response.domain.enums.VerificationStatus;
import com.disaster.response.dto.report.AdminDashboardResponse;
import com.disaster.response.dto.report.DataSourceStatusResponse;
import com.disaster.response.dto.report.ResponsePerformanceReport;
import com.disaster.response.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

@Service
public class ReportingService {

    private final IncidentRepository incidentRepository;
    private final IncidentAssignmentRepository assignmentRepository;
    private final ResponderTeamRepository teamRepository;
    private final DisasterAlertRepository alertRepository;
    private final UserRepository userRepository;
    private final DataSourceRunRepository runRepository;
    private final ModelRegistryRepository modelRegistryRepository;

    public ReportingService(
            IncidentRepository incidentRepository,
            IncidentAssignmentRepository assignmentRepository,
            ResponderTeamRepository teamRepository,
            DisasterAlertRepository alertRepository,
            UserRepository userRepository,
            DataSourceRunRepository runRepository,
            ModelRegistryRepository modelRegistryRepository) {
        this.incidentRepository = incidentRepository;
        this.assignmentRepository = assignmentRepository;
        this.teamRepository = teamRepository;
        this.alertRepository = alertRepository;
        this.userRepository = userRepository;
        this.runRepository = runRepository;
        this.modelRegistryRepository = modelRegistryRepository;
    }

    @Transactional(readOnly = true)
    public AdminDashboardResponse getAdminDashboard() {
        long totalIncidents = incidentRepository.count();
        long pendingIncidents = incidentRepository.countByVerificationStatus(VerificationStatus.PENDING);
        long verifiedIncidents = incidentRepository.countByVerificationStatus(VerificationStatus.VERIFIED);
        long rejectedIncidents = incidentRepository.countByVerificationStatus(VerificationStatus.REJECTED);

        Map<String, Long> bySeverity = new HashMap<>();
        for (IncidentSeverity s : IncidentSeverity.values()) {
            bySeverity.put(s.name(), incidentRepository.countBySeverity(s));
        }

        long activeAlerts = alertRepository.findActiveAlerts(Instant.now()).size();
        long totalTeams = teamRepository.count();
        long availableTeams = teamRepository.findByAvailabilityStatus(AvailabilityStatus.AVAILABLE).size();
        long deployedTeams = teamRepository.findByAvailabilityStatus(AvailabilityStatus.DEPLOYED).size()
                + teamRepository.findByAvailabilityStatus(AvailabilityStatus.DISPATCHED).size();

        long activeAssignments = assignmentRepository.findActiveAssignments().size();
        long totalUsers = userRepository.count();

        String modelStatus = modelRegistryRepository.findByActiveTrue()
                .map(m -> "ACTIVE: " + m.getModelName() + " (" + m.getVersion() + ")")
                .orElse("DEVELOPMENT_MOCK_READY");

        String dataFreshness = "MONITORED";

        return new AdminDashboardResponse(
                totalIncidents,
                pendingIncidents,
                verifiedIncidents,
                rejectedIncidents,
                bySeverity,
                activeAlerts,
                totalTeams,
                availableTeams,
                deployedTeams,
                activeAssignments,
                totalUsers,
                modelStatus,
                dataFreshness
        );
    }

    @Transactional(readOnly = true)
    public ResponsePerformanceReport getPerformanceReport() {
        List<IncidentAssignment> allAssignments = assignmentRepository.findAll();

        long total = allAssignments.size();
        long resolved = allAssignments.stream().filter(a -> a.getAssignmentStatus() == AssignmentStatus.RESOLVED).count();
        long cancelled = allAssignments.stream().filter(a -> a.getAssignmentStatus() == AssignmentStatus.CANCELLED).count();
        long inProgress = total - resolved - cancelled;

        Map<String, Long> byTeam = new HashMap<>();
        Map<String, Long> byStatus = new HashMap<>();
        List<Double> dispatchDurations = new ArrayList<>();
        List<Double> resolutionDurations = new ArrayList<>();

        for (IncidentAssignment a : allAssignments) {
            String teamName = a.getTeam() != null ? a.getTeam().getName() : "Unknown";
            byTeam.put(teamName, byTeam.getOrDefault(teamName, 0L) + 1);

            String status = a.getAssignmentStatus().name();
            byStatus.put(status, byStatus.getOrDefault(status, 0L) + 1);

            if (a.getIncident() != null && a.getApprovalTime() != null) {
                long dispatchMinutes = Duration.between(a.getIncident().getReportedTime(), a.getApprovalTime()).toMinutes();
                dispatchDurations.add((double) Math.max(0, dispatchMinutes));
            }

            if (a.getCompletionTime() != null && a.getAssignmentTime() != null) {
                double resHours = Duration.between(a.getAssignmentTime(), a.getCompletionTime()).toMinutes() / 60.0;
                resolutionDurations.add(Math.max(0.0, resHours));
            }
        }

        Double avgDispatch = dispatchDurations.isEmpty() ? null :
                dispatchDurations.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);

        Double avgResolution = resolutionDurations.isEmpty() ? null :
                resolutionDurations.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);

        return new ResponsePerformanceReport(
                total,
                resolved,
                cancelled,
                inProgress,
                avgDispatch != null ? Math.round(avgDispatch * 10.0) / 10.0 : null,
                avgResolution != null ? Math.round(avgResolution * 10.0) / 10.0 : null,
                byTeam,
                byStatus
        );
    }

    @Transactional(readOnly = true)
    public DataSourceStatusResponse getDataSourceStatus() {
        List<String> providers = List.of("OPEN_METEO", "USGS", "GDACS", "IMD");
        List<DataSourceStatusResponse.ProviderStatusDto> list = new ArrayList<>();

        for (String provider : providers) {
            Optional<DataSourceRun> runOpt = runRepository.findFirstByProviderOrderByStartTimeDesc(provider);
            if (runOpt.isPresent()) {
                list.add(DataSourceStatusResponse.ProviderStatusDto.from(runOpt.get()));
            } else {
                list.add(new DataSourceStatusResponse.ProviderStatusDto(
                        null,
                        provider,
                        "IDLE",
                        null,
                        com.disaster.response.domain.enums.RunStatus.SUCCESS,
                        0,
                        "Provider configured and awaiting scheduled trigger",
                        true
                ));
            }
        }

        return new DataSourceStatusResponse(list);
    }
}
