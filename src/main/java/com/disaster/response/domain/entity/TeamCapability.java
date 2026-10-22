package com.disaster.response.domain.entity;

import com.disaster.response.domain.enums.CapabilityType;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "team_capabilities")
public class TeamCapability {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id", nullable = false)
    private ResponderTeam team;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CapabilityType capability;

    @Column(name = "equipment_metadata", columnDefinition = "TEXT")
    private String equipmentMetadata;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public TeamCapability() {
    }

    public TeamCapability(ResponderTeam team, CapabilityType capability, String equipmentMetadata) {
        this.team = team;
        this.capability = capability;
        this.equipmentMetadata = equipmentMetadata;
        this.createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public ResponderTeam getTeam() {
        return team;
    }

    public void setTeam(ResponderTeam team) {
        this.team = team;
    }

    public CapabilityType getCapability() {
        return capability;
    }

    public void setCapability(CapabilityType capability) {
        this.capability = capability;
    }

    public String getEquipmentMetadata() {
        return equipmentMetadata;
    }

    public void setEquipmentMetadata(String equipmentMetadata) {
        this.equipmentMetadata = equipmentMetadata;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
