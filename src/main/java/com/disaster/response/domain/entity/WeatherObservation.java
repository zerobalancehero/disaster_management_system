package com.disaster.response.domain.entity;

import com.disaster.response.domain.enums.WeatherCondition;
import jakarta.persistence.*;
import org.locationtech.jts.geom.Point;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "weather_observations")
public class WeatherObservation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String source;

    @Column(name = "external_location_id")
    private String externalLocationId;

    @Column(columnDefinition = "geometry(Point, 4326)", nullable = false)
    private Point coordinates;

    @Column(name = "observation_time", nullable = false)
    private Instant observationTime;

    @Column
    private Double temperature;

    @Column
    private Double humidity;

    @Column
    private Double rainfall;

    @Column(name = "wind_speed")
    private Double windSpeed;

    @Enumerated(EnumType.STRING)
    @Column(name = "weather_condition")
    private WeatherCondition weatherCondition;

    @Column(name = "raw_payload", columnDefinition = "TEXT")
    private String rawPayload;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public WeatherObservation() {
    }

    public WeatherObservation(String source, String externalLocationId, Point coordinates, Instant observationTime,
                              Double temperature, Double humidity, Double rainfall, Double windSpeed,
                              WeatherCondition weatherCondition, String rawPayload) {
        this.source = source;
        this.externalLocationId = externalLocationId;
        this.coordinates = coordinates;
        this.observationTime = observationTime;
        this.temperature = temperature;
        this.humidity = humidity;
        this.rainfall = rainfall;
        this.windSpeed = windSpeed;
        this.weatherCondition = weatherCondition;
        this.rawPayload = rawPayload;
        this.createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getExternalLocationId() {
        return externalLocationId;
    }

    public void setExternalLocationId(String externalLocationId) {
        this.externalLocationId = externalLocationId;
    }

    public Point getCoordinates() {
        return coordinates;
    }

    public void setCoordinates(Point coordinates) {
        this.coordinates = coordinates;
    }

    public Instant getObservationTime() {
        return observationTime;
    }

    public void setObservationTime(Instant observationTime) {
        this.observationTime = observationTime;
    }

    public Double getTemperature() {
        return temperature;
    }

    public void setTemperature(Double temperature) {
        this.temperature = temperature;
    }

    public Double getHumidity() {
        return humidity;
    }

    public void setHumidity(Double humidity) {
        this.humidity = humidity;
    }

    public Double getRainfall() {
        return rainfall;
    }

    public void setRainfall(Double rainfall) {
        this.rainfall = rainfall;
    }

    public Double getWindSpeed() {
        return windSpeed;
    }

    public void setWindSpeed(Double windSpeed) {
        this.windSpeed = windSpeed;
    }

    public WeatherCondition getWeatherCondition() {
        return weatherCondition;
    }

    public void setWeatherCondition(WeatherCondition weatherCondition) {
        this.weatherCondition = weatherCondition;
    }

    public String getRawPayload() {
        return rawPayload;
    }

    public void setRawPayload(String rawPayload) {
        this.rawPayload = rawPayload;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
