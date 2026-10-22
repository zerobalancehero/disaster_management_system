package com.disaster.response.config;

import com.disaster.response.domain.entity.*;
import com.disaster.response.domain.enums.*;
import com.disaster.response.gis.GisUtil;
import com.disaster.response.repository.*;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Component
public class DevDataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DevDataSeeder.class);

    private final UserRepository userRepository;
    private final ResponderTeamRepository teamRepository;
    private final TeamMemberRepository memberRepository;
    private final TeamCapabilityRepository capabilityRepository;
    private final RegionRepository regionRepository;
    private final TerrainFeatureRepository terrainRepository;
    private final IncidentRepository incidentRepository;
    private final DisasterAlertRepository alertRepository;
    private final WeatherObservationRepository observationRepository;
    private final WeatherForecastRepository forecastRepository;
    private final SafetyInstructionRepository instructionRepository;
    private final RiskAssessmentRepository riskAssessmentRepository;
    private final NotificationRepository notificationRepository;
    private final PasswordEncoder passwordEncoder;
    private final boolean seedEnabled;

    public DevDataSeeder(
            UserRepository userRepository,
            ResponderTeamRepository teamRepository,
            TeamMemberRepository memberRepository,
            TeamCapabilityRepository capabilityRepository,
            RegionRepository regionRepository,
            TerrainFeatureRepository terrainRepository,
            IncidentRepository incidentRepository,
            DisasterAlertRepository alertRepository,
            WeatherObservationRepository observationRepository,
            WeatherForecastRepository forecastRepository,
            SafetyInstructionRepository instructionRepository,
            RiskAssessmentRepository riskAssessmentRepository,
            NotificationRepository notificationRepository,
            PasswordEncoder passwordEncoder,
            @Value("${app.seed.enabled:false}") boolean seedEnabled) {
        this.userRepository = userRepository;
        this.teamRepository = teamRepository;
        this.memberRepository = memberRepository;
        this.capabilityRepository = capabilityRepository;
        this.regionRepository = regionRepository;
        this.terrainRepository = terrainRepository;
        this.incidentRepository = incidentRepository;
        this.alertRepository = alertRepository;
        this.observationRepository = observationRepository;
        this.forecastRepository = forecastRepository;
        this.instructionRepository = instructionRepository;
        this.riskAssessmentRepository = riskAssessmentRepository;
        this.notificationRepository = notificationRepository;
        this.passwordEncoder = passwordEncoder;
        this.seedEnabled = seedEnabled;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (!seedEnabled) {
            log.info("Development seeding is DISABLED (app.seed.enabled=false). Skipping.");
            return;
        }

        if (userRepository.count() > 0) {
            log.info("Database already contains user records. Skipping dev data seeding to avoid duplicates.");
            return;
        }

        log.info("Seeding synthetic development data for AI Disaster Response System...");

        // 1. Users
        User admin = new User("System Admin", "admin@disaster.local",
                passwordEncoder.encode("Admin@123"), "+91-9876543210", UserRole.ADMIN);
        User responder = new User("Captain Rajesh Kumar", "responder@disaster.local",
                passwordEncoder.encode("Responder@123"), "+91-9876543211", UserRole.RESPONDER);
        User citizen = new User("Ananya Sharma", "user@disaster.local",
                passwordEncoder.encode("User@123"), "+91-9876543212", UserRole.USER);

        admin = userRepository.save(admin);
        responder = userRepository.save(responder);
        citizen = userRepository.save(citizen);

        // 2. Regions & Terrain Features (Chennai Coastal / Flood Zone synthetic area)
        GeometryFactory gf = GisUtil.getGeometryFactory();
        Coordinate[] coords = new Coordinate[]{
                new Coordinate(80.20, 12.90),
                new Coordinate(80.35, 12.90),
                new Coordinate(80.35, 13.15),
                new Coordinate(80.20, 13.15),
                new Coordinate(80.20, 12.90)
        };
        Geometry boundary = gf.createPolygon(coords);
        Point center = GisUtil.createPoint(13.025, 80.275);

        Region coastalZone = new Region("Chennai Metropolitan Coastal Region", RegionType.COASTAL_ZONE, boundary, center);
        coastalZone = regionRepository.save(coastalZone);

        TerrainFeature terrain = new TerrainFeature(coastalZone, 8.5, 2.1, 450.0, 120.0, "SYNTHETIC_COPERNICUS_DEM");
        terrainRepository.save(terrain);

        // 3. Responder Teams
        Point base1 = GisUtil.createPoint(13.0827, 80.2707); // Central Base
        ResponderTeam teamAlpha = new ResponderTeam("Alpha Water Rescue Squad", TeamType.SEARCH_AND_RESCUE,
                AvailabilityStatus.AVAILABLE, base1, "alpha.ops@rescue.gov");
        teamAlpha = teamRepository.save(teamAlpha);
        capabilityRepository.save(new TeamCapability(teamAlpha, CapabilityType.WATER_RESCUE, "Inflatable rescue boats x 4"));
        capabilityRepository.save(new TeamCapability(teamAlpha, CapabilityType.BOAT_OPERATIONS, "Outboard motors & sonar"));
        capabilityRepository.save(new TeamCapability(teamAlpha, CapabilityType.EVACUATION_LOGISTICS, "High clearance rescue trucks x 2"));

        Point base2 = GisUtil.createPoint(13.0500, 80.2100);
        ResponderTeam teamBravo = new ResponderTeam("Bravo Urban Search & Rescue", TeamType.SEARCH_AND_RESCUE,
                AvailabilityStatus.AVAILABLE, base2, "bravo.ops@rescue.gov");
        teamBravo = teamRepository.save(teamBravo);
        capabilityRepository.save(new TeamCapability(teamBravo, CapabilityType.COLLAPSED_STRUCTURE, "Hydraulic cutters & acoustic listening devices"));
        capabilityRepository.save(new TeamCapability(teamBravo, CapabilityType.CANINE_SEARCH, "Certified disaster search dogs x 2"));
        capabilityRepository.save(new TeamCapability(teamBravo, CapabilityType.FIRST_AID, "Field trauma medical kits"));

        Point base3 = GisUtil.createPoint(13.0100, 80.2300);
        ResponderTeam teamCharlie = new ResponderTeam("Charlie Medical Response Unit", TeamType.MEDICAL,
                AvailabilityStatus.AVAILABLE, base3, "charlie.ops@rescue.gov");
        teamCharlie = teamRepository.save(teamCharlie);
        capabilityRepository.save(new TeamCapability(teamCharlie, CapabilityType.ADVANCED_LIFE_SUPPORT, "Mobile ICU ambulances x 3"));
        capabilityRepository.save(new TeamCapability(teamCharlie, CapabilityType.FIRST_AID, "Triage tents & ventilators"));

        // Assign responder to Team Alpha
        memberRepository.save(new TeamMember(teamAlpha, responder, MembershipStatus.ACTIVE));

        // 4. Incidents
        Point inc1Loc = GisUtil.createPoint(13.0600, 80.2400);
        Incident inc1 = new Incident(citizen, "Severe Waterlogging and Trapped Families",
                "Floodwaters entered ground floor residential homes in Velachery after 150mm heavy rainfall. 8 people stranded on roof.",
                DisasterType.FLOOD, IncidentSeverity.HIGH, inc1Loc);
        inc1.setVerificationStatus(VerificationStatus.VERIFIED);
        inc1.setVerifier(admin);
        inc1.setVerificationTime(Instant.now().minus(30, ChronoUnit.MINUTES));
        incidentRepository.save(inc1);

        Point inc2Loc = GisUtil.createPoint(13.0400, 80.2500);
        Incident inc2 = new Incident(citizen, "Structural Wall Collapse near Commercial Market",
                "Cracking and partial wall collapse observed following earth tremors.",
                DisasterType.EARTHQUAKE, IncidentSeverity.MEDIUM, inc2Loc);
        inc2.setVerificationStatus(VerificationStatus.PENDING);
        incidentRepository.save(inc2);

        Point inc3Loc = GisUtil.createPoint(13.0700, 80.2600);
        Incident inc3 = new Incident(citizen, "Minor Water Puddle - Duplicate Alarm",
                "False alarm regarding normal street drain overflow.",
                DisasterType.FLOOD, IncidentSeverity.LOW, inc3Loc);
        inc3.setVerificationStatus(VerificationStatus.REJECTED);
        inc3.setVerifier(admin);
        inc3.setVerificationTime(Instant.now().minus(2, ChronoUnit.HOURS));
        inc3.setRejectionReason("Inspected and determined to be normal municipal drainage with no immediate threat to life.");
        incidentRepository.save(inc3);

        // 5. Weather Observations & Forecasts
        observationRepository.save(new WeatherObservation(
                "SYNTHETIC_OPEN_METEO", "13.08,80.27", base1, Instant.now().minus(15, ChronoUnit.MINUTES),
                29.4, 84.0, 78.5, 42.0, WeatherCondition.HEAVY_RAIN, "{ \"synthetic\": true }"
        ));

        forecastRepository.save(new WeatherForecast(
                "SYNTHETIC_OPEN_METEO", base1, coastalZone, Instant.now(), Instant.now().plus(6, ChronoUnit.HOURS),
                28.0, 55.0, 48.0, WeatherCondition.THUNDERSTORM
        ));

        // 6. Disaster Alerts
        alertRepository.save(new DisasterAlert(
                "SYSTEM_SYNTHETIC", "ALERT-2026-FL-001", DisasterType.FLOOD, AlertSeverity.SEVERE,
                "Flash Flood & Inundation Warning for Low-lying Coastal Areas",
                "Heavy to very heavy rainfall exceeding 100mm expected. Avoid non-essential travel in underpass areas.",
                boundary, Instant.now().minus(2, ChronoUnit.HOURS), Instant.now().plus(24, ChronoUnit.HOURS),
                AlertStatus.ACTIVE, "{ \"synthetic\": true }"
        ));

        // 7. Safety Instructions
        instructionRepository.save(new SafetyInstruction(
                "Flash Flood Evacuation & Electrical Safety", DisasterType.FLOOD, AlertSeverity.LOW,
                "1. Immediately move to higher floors. 2. Switch off the main electrical breaker. 3. Do not attempt to walk or drive through flowing water.",
                TargetAudience.GENERAL_PUBLIC, "en", admin
        ));

        instructionRepository.save(new SafetyInstruction(
                "Earthquake Drop, Cover, and Hold Protocol", DisasterType.EARTHQUAKE, AlertSeverity.LOW,
                "1. Drop onto your hands and knees. 2. Cover your head and neck under a sturdy table or desk. 3. Hold on until shaking stops.",
                TargetAudience.GENERAL_PUBLIC, "en", admin
        ));

        // 8. Risk Assessment (clearly marked mock)
        riskAssessmentRepository.save(new RiskAssessment(
                coastalZone, DisasterType.FLOOD, 0.74, RiskCategory.HIGH, PredictionHorizon.NEXT_24H,
                "SYNTHETIC-MOCK-v1.0", "{ \"rainfallMm\": 78.5, \"elevation\": 8.5 }",
                "{ \"synthetic\": true, \"note\": \"Development mock assessment\" }"
        ));

        // 9. Notifications
        notificationRepository.save(new Notification(
                responder, NotificationType.DISPATCH_ALERT, "Welcome to Emergency Response System",
                "You have been registered as an active responder for Alpha Water Rescue Squad.",
                "ResponderTeam", teamAlpha.getId().toString()
        ));

        log.info("Development seeding successfully completed!");
        log.info("Default seed accounts created:");
        log.info("  ADMIN:     admin@disaster.local     / Admin@123");
        log.info("  RESPONDER: responder@disaster.local / Responder@123");
        log.info("  USER:      user@disaster.local      / User@123");
    }
}
