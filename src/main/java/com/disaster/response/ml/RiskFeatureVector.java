package com.disaster.response.ml;

import com.disaster.response.domain.enums.DisasterType;
import com.disaster.response.domain.enums.PredictionHorizon;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record RiskFeatureVector(
    @NotNull(message = "Region ID is required")
    UUID regionId,

    @NotNull(message = "Disaster type is required")
    DisasterType disasterType,

    @NotNull(message = "Prediction horizon is required")
    PredictionHorizon predictionHorizon,

    @Min(value = -100, message = "Temperature out of realistic range")
    @Max(value = 80, message = "Temperature out of realistic range")
    Double temperatureCelsius,

    @Min(value = 0, message = "Humidity cannot be negative")
    @Max(value = 100, message = "Humidity cannot exceed 100")
    Double humidityPercent,

    @Min(value = 0, message = "Rainfall cannot be negative")
    @Max(value = 2000, message = "Rainfall exceeds realistic threshold")
    Double rainfallMm,

    @Min(value = 0, message = "Wind speed cannot be negative")
    @Max(value = 500, message = "Wind speed exceeds realistic threshold")
    Double windSpeedKmh,

    Double elevationMeters,

    @Min(value = 0, message = "Slope cannot be negative")
    @Max(value = 90, message = "Slope cannot exceed 90 degrees")
    Double slopeDegrees,

    Double flowAccumulation,

    @Min(value = 0, message = "Distance cannot be negative")
    Double distanceToRiverMeters
) {
    public static final String SCHEMA_VERSION = "v1.0";

    public void validate() {
        if (regionId == null) {
            throw new IllegalArgumentException("Region ID cannot be null");
        }
        if (disasterType == null) {
            throw new IllegalArgumentException("Disaster type cannot be null");
        }
        if (predictionHorizon == null) {
            throw new IllegalArgumentException("Prediction horizon cannot be null");
        }
        if (humidityPercent != null && (humidityPercent < 0 || humidityPercent > 100)) {
            throw new IllegalArgumentException("Humidity must be between 0 and 100%");
        }
        if (slopeDegrees != null && (slopeDegrees < 0 || slopeDegrees > 90)) {
            throw new IllegalArgumentException("Slope must be between 0 and 90 degrees");
        }
    }
}
