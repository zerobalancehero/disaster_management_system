package com.disaster.response.domain.entity;

import com.disaster.response.domain.enums.RegionType;
import jakarta.persistence.*;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "regions")
public class Region {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "region_type", nullable = false)
    private RegionType regionType;

    @Column(columnDefinition = "geometry(Geometry, 4326)", nullable = false)
    private Geometry boundary;

    @Column(name = "center_point", columnDefinition = "geometry(Point, 4326)", nullable = false)
    private Point centerPoint;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public Region() {
    }

    public Region(String name, RegionType regionType, Geometry boundary, Point centerPoint) {
        this.name = name;
        this.regionType = regionType;
        this.boundary = boundary;
        this.centerPoint = centerPoint;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = Instant.now();
    }

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

    public RegionType getRegionType() {
        return regionType;
    }

    public void setRegionType(RegionType regionType) {
        this.regionType = regionType;
    }

    public Geometry getBoundary() {
        return boundary;
    }

    public void setBoundary(Geometry boundary) {
        this.boundary = boundary;
    }

    public Point getCenterPoint() {
        return centerPoint;
    }

    public void setCenterPoint(Point centerPoint) {
        this.centerPoint = centerPoint;
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
