package com.disaster.response.domain.entity;

import com.disaster.response.domain.enums.WeatherCondition;
import jakarta.persistence.*;
import org.locationtech.jts.geom.Point;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "weather_forecasts")
public class WeatherForecast {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String source;

    @Column(columnDefinition = "geometry(Point, 4326)", nullable = false)
    private Point coordinates;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "region_id")
    private Region region;

    @Column(name = "forecast_issue_time", nullable = false)
    private Instant forecastIssueTime;

    @Column(name = "forecast_target_time", nullable = false)
    private Instant forecastTargetTime;

    @Column
    private Double temperature;

    @Column
    private Double rainfall;

    @Column(name = "wind_speed")
    private Double windSpeed;

    @Enumerated(EnumType.STRING)
    @Column(name = "weather_condition")
    private WeatherCondition weatherCondition;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public WeatherForecast() {
    }

    public WeatherForecast(String source, Point coordinates, Region region, Instant forecastIssueTime,
                           Instant forecastTargetTime, Double temperature, Double rainfall,
                           Double windSpeed, WeatherCondition weatherCondition) {
        this.source = source;
        this.coordinates = coordinates;
        this.region = region;
        this.forecastIssueTime = forecastIssueTime;
        this.forecastTargetTime = forecastTargetTime;
        this.temperature = temperature;
        this.rainfall = rainfall;
        this.windSpeed = windSpeed;
        this.weatherCondition = weatherCondition;
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

    public Point getCoordinates() {
        return coordinates;
    }

    public void setCoordinates(Point coordinates) {
        this.coordinates = coordinates;
    }

    public Region getRegion() {
        return region;
    }

    public void setRegion(Region region) {
        this.region = region;
    }

    public Instant getForecastIssueTime() {
        return forecastIssueTime;
    }

    public void setForecastIssueTime(Instant forecastIssueTime) {
        this.forecastIssueTime = forecastIssueTime;
    }

    public Instant getForecastTargetTime() {
        return forecastTargetTime;
    }

    public void setForecastTargetTime(Instant forecastTargetTime) {
        this.forecastTargetTime = forecastTargetTime;
    }

    public Double getTemperature() {
        return temperature;
    }

    public void setTemperature(Double temperature) {
        this.temperature = temperature;
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

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
