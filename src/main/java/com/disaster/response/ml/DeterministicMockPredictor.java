package com.disaster.response.ml;

import com.disaster.response.domain.enums.RiskCategory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Deterministic Mock Predictor used ONLY in development/test profiles.
 * Clearly marks predictions as synthetic and uses deterministic heuristic formulas.
 */
@Component
@ConditionalOnProperty(name = "app.ml.mock-enabled", havingValue = "true")
public class DeterministicMockPredictor implements DisasterRiskPredictor {

    public static final String MOCK_VERSION = "SYNTHETIC-MOCK-v1.0";

    @Override
    public RiskPredictionResult predict(RiskFeatureVector features) {
        features.validate();

        double score = 0.10; // Baseline ambient probability

        if (features.rainfallMm() != null) {
            score += Math.min(0.50, features.rainfallMm() / 200.0);
        }
        if (features.windSpeedKmh() != null) {
            score += Math.min(0.25, features.windSpeedKmh() / 150.0);
        }
        if (features.slopeDegrees() != null && features.slopeDegrees() > 25.0) {
            score += 0.15;
        }

        // Clamp between 0.05 and 0.98
        double probability = Math.max(0.05, Math.min(0.98, score));
        RiskCategory category = RiskCategory.fromProbability(probability);

        Map<String, Object> featureSnapshot = new HashMap<>();
        featureSnapshot.put("rainfallMm", features.rainfallMm());
        featureSnapshot.put("windSpeedKmh", features.windSpeedKmh());
        featureSnapshot.put("slopeDegrees", features.slopeDegrees());
        featureSnapshot.put("elevationMeters", features.elevationMeters());

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("synthetic", true);
        metadata.put("environment", "DEVELOPMENT_TEST");
        metadata.put("heuristicWeightRainfall", 0.50);

        return new RiskPredictionResult(
                features.regionId(),
                features.disasterType(),
                probability,
                category,
                features.predictionHorizon(),
                Instant.now(),
                MOCK_VERSION,
                "DETERMINISTIC_MOCK",
                featureSnapshot,
                metadata
        );
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public String getModelVersion() {
        return MOCK_VERSION;
    }

    @Override
    public String getFramework() {
        return "DETERMINISTIC_MOCK";
    }
}
