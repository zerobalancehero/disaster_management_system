package com.disaster.response.domain.entity;

import com.disaster.response.domain.enums.AvailabilityStatus;
import com.disaster.response.domain.enums.TeamType;
import jakarta.persistence.*;
import org.locationtech.jts.geom.Point;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "responder_teams")
public class ResponderTeam {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "team_type", nullable = false)
    private TeamType teamType;

    @Enumerated(EnumType.STRING)
    @Column(name = "availability_status", nullable = false)
    private AvailabilityStatus availabilityStatus = AvailabilityStatus.AVAILABLE;

    @Column(name = "base_location", columnDefinition = "geometry(Point, 4326)", nullable = false)
    private Point baseLocation;

    @Column(name = "contact_details")
    private String contactDetails;

    @OneToMany(mappedBy = "team", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TeamMember> members = new ArrayList<>();

    @OneToMany(mappedBy = "team", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TeamCapability> capabilities = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public ResponderTeam() {
    }

    public ResponderTeam(String name, TeamType teamType, AvailabilityStatus status, Point baseLocation, String contactDetails) {
        this.name = name;
        this.teamType = teamType;
        this.availabilityStatus = status != null ? status : AvailabilityStatus.AVAILABLE;
        this.baseLocation = baseLocation;
        this.contactDetails = contactDetails;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = Instant.now();
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public TeamType getTeamType() {
        return teamType;
    }

    public void setTeamType(TeamType teamType) {
        this.teamType = teamType;
    }

    public AvailabilityStatus getAvailabilityStatus() {
        return availabilityStatus;
    }

    public void setAvailabilityStatus(AvailabilityStatus availabilityStatus) {
        this.availabilityStatus = availabilityStatus;
    }

    public Point getBaseLocation() {
        return baseLocation;
    }

    public void setBaseLocation(Point baseLocation) {
        this.baseLocation = baseLocation;
    }

    public String getContactDetails() {
        return contactDetails;
    }

    public void setContactDetails(String contactDetails) {
        this.contactDetails = contactDetails;
    }

    public List<TeamMember> getMembers() {
        return members;
    }

    public void setMembers(List<TeamMember> members) {
        this.members = members;
    }

    public List<TeamCapability> getCapabilities() {
        return capabilities;
    }

    public void setCapabilities(List<TeamCapability> capabilities) {
        this.capabilities = capabilities;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
