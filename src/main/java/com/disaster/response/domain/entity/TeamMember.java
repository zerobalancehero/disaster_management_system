package com.disaster.response.domain.entity;

import com.disaster.response.domain.enums.MembershipStatus;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "team_members", uniqueConstraints = {
    @UniqueConstraint(name = "uk_team_user_active", columnNames = {"team_id", "user_id"})
})
public class TeamMember {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id", nullable = false)
    private ResponderTeam team;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "membership_status", nullable = false)
    private MembershipStatus membershipStatus = MembershipStatus.ACTIVE;

    @Column(name = "joined_date", nullable = false)
    private Instant joinedDate = Instant.now();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public TeamMember() {
    }

    public TeamMember(ResponderTeam team, User user, MembershipStatus status) {
        this.team = team;
        this.user = user;
        this.membershipStatus = status != null ? status : MembershipStatus.ACTIVE;
        this.joinedDate = Instant.now();
        this.createdAt = Instant.now();
    }

    // Getters and Setters
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

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public MembershipStatus getMembershipStatus() {
        return membershipStatus;
    }

    public void setMembershipStatus(MembershipStatus membershipStatus) {
        this.membershipStatus = membershipStatus;
    }

    public Instant getJoinedDate() {
        return joinedDate;
    }

    public void setJoinedDate(Instant joinedDate) {
        this.joinedDate = joinedDate;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
