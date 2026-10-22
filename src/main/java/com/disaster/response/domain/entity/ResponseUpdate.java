package com.disaster.response.domain.entity;

import jakarta.persistence.*;
import org.locationtech.jts.geom.Point;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "response_updates")
public class ResponseUpdate {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignment_id", nullable = false)
    private IncidentAssignment assignment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_id", nullable = false)
    private User actor;

    @Column(nullable = false)
    private String status;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String message;

    @Column(columnDefinition = "geometry(Point, 4326)")
    private Point location;

    @Column(nullable = false)
    private Instant timestamp = Instant.now();

    public ResponseUpdate() {
    }

    public ResponseUpdate(IncidentAssignment assignment, User actor, String status, String message, Point location) {
        this.assignment = assignment;
        this.actor = actor;
        this.status = status;
        this.message = message;
        this.location = location;
        this.timestamp = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public IncidentAssignment getAssignment() {
        return assignment;
    }

    public void setAssignment(IncidentAssignment assignment) {
        this.assignment = assignment;
    }

    public User getActor() {
        return actor;
    }

    public void setActor(User actor) {
        this.actor = actor;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Point getLocation() {
        return location;
    }

    public void setLocation(Point location) {
        this.location = location;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }
}
