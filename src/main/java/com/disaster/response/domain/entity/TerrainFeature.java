package com.disaster.response.domain.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "terrain_features")
public class TerrainFeature {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "region_id", nullable = false)
    private Region region;

    @Column(nullable = false)
    private Double elevation;

    @Column
    private Double slope;

    @Column(name = "flow_accumulation")
    private Double flowAccumulation;

    @Column(name = "distance_to_river")
    private Double distanceToRiver;

    @Column(name = "data_source", nullable = false)
    private String dataSource;

    @Column(name = "processing_time", nullable = false)
    private Instant processingTime = Instant.now();

    public TerrainFeature() {
    }

    public TerrainFeature(Region region, Double elevation, Double slope, Double flowAccumulation,
                          Double distanceToRiver, String dataSource) {
        this.region = region;
        this.elevation = elevation;
        this.slope = slope;
        this.flowAccumulation = flowAccumulation;
        this.distanceToRiver = distanceToRiver;
        this.dataSource = dataSource;
        this.processingTime = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Region getRegion() {
        return region;
    }

    public void setRegion(Region region) {
        this.region = region;
    }

    public Double getElevation() {
        return elevation;
    }

    public void setElevation(Double elevation) {
        this.elevation = elevation;
    }

    public Double getSlope() {
        return slope;
    }

    public void setSlope(Double slope) {
        this.slope = slope;
    }

    public Double getFlowAccumulation() {
        return flowAccumulation;
    }

    public void setFlowAccumulation(Double flowAccumulation) {
        this.flowAccumulation = flowAccumulation;
    }

    public Double getDistanceToRiver() {
        return distanceToRiver;
    }

    public void setDistanceToRiver(Double distanceToRiver) {
        this.distanceToRiver = distanceToRiver;
    }

    public String getDataSource() {
        return dataSource;
    }

    public void setDataSource(String dataSource) {
        this.dataSource = dataSource;
    }

    public Instant getProcessingTime() {
        return processingTime;
    }

    public void setProcessingTime(Instant processingTime) {
        this.processingTime = processingTime;
    }
}
