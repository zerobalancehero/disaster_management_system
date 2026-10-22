package com.disaster.response;

import com.disaster.response.domain.enums.DisasterType;
import com.disaster.response.domain.enums.PredictionHorizon;
import com.disaster.response.domain.enums.RiskCategory;
import com.disaster.response.exception.ModelUnavailableException;
import com.disaster.response.ml.DeterministicMockPredictor;
import com.disaster.response.ml.ProductionModelPredictor;
import com.disaster.response.ml.RiskFeatureVector;
import com.disaster.response.ml.RiskPredictionResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class MlInferenceAndFeatureValidationTest {

    @Test
    @DisplayName("ML Feature Validation: Null regionId throws IllegalArgumentException")
    void testNullRegionThrowsException() {
        RiskFeatureVector vector = new RiskFeatureVector(
                null, DisasterType.FLOOD, PredictionHorizon.NEXT_24H,
                25.0, 80.0, 50.0, 20.0, 10.0, 5.0, 100.0, 50.0
        );
        assertThrows(IllegalArgumentException.class, vector::validate);
    }

    @Test
    @DisplayName("ML Feature Validation: Out-of-range humidity (>100%) throws IllegalArgumentException")
    void testInvalidHumidityThrowsException() {
        RiskFeatureVector vector = new RiskFeatureVector(
                UUID.randomUUID(), DisasterType.FLOOD, PredictionHorizon.NEXT_24H,
                25.0, 120.0, 50.0, 20.0, 10.0, 5.0, 100.0, 50.0
        );
        assertThrows(IllegalArgumentException.class, vector::validate);
    }

    @Test
    @DisplayName("ML Feature Validation: Out-of-range slope (>90 deg) throws IllegalArgumentException")
    void testInvalidSlopeThrowsException() {
        RiskFeatureVector vector = new RiskFeatureVector(
                UUID.randomUUID(), DisasterType.LANDSLIDE, PredictionHorizon.NEXT_24H,
                25.0, 80.0, 50.0, 20.0, 10.0, 95.0, 100.0, 50.0
        );
        assertThrows(IllegalArgumentException.class, vector::validate);
    }

    @Test
    @DisplayName("Rule 15: Missing production model artifact NEVER produces fake predictions")
    void testMissingProductionModelArtifactThrowsModelUnavailableException() {
        // Instantiate production predictor with non-existent model path
        ProductionModelPredictor prodPredictor = new ProductionModelPredictor("/non/existent/path/model.onnx");

        assertFalse(prodPredictor.isAvailable());
        assertEquals("UNAVAILABLE", prodPredictor.getModelVersion());

        RiskFeatureVector validVector = new RiskFeatureVector(
                UUID.randomUUID(), DisasterType.FLOOD, PredictionHorizon.NEXT_24H,
                28.0, 85.0, 150.0, 35.0, 12.0, 4.0, 250.0, 30.0
        );

        ModelUnavailableException ex = assertThrows(ModelUnavailableException.class, () ->
                prodPredictor.predict(validVector)
        );
        assertTrue(ex.getMessage().contains("not loaded"));
    }

    @Test
    @DisplayName("Deterministic Mock Predictor: Deterministic, marked synthetic, with correct category mapping")
    void testDeterministicMockPredictor() {
        DeterministicMockPredictor mockPredictor = new DeterministicMockPredictor();
        assertTrue(mockPredictor.isAvailable());
        assertEquals(DeterministicMockPredictor.MOCK_VERSION, mockPredictor.getModelVersion());

        // Extreme rainfall scenario should produce HIGH or CRITICAL
        RiskFeatureVector extremeVector = new RiskFeatureVector(
                UUID.randomUUID(), DisasterType.FLOOD, PredictionHorizon.NEXT_24H,
                28.0, 95.0, 180.0, 85.0, 5.0, 30.0, 500.0, 10.0
        );

        RiskPredictionResult result = mockPredictor.predict(extremeVector);
        assertNotNull(result);
        assertTrue(result.probability() >= 0.70);
        assertTrue(result.riskCategory() == RiskCategory.HIGH || result.riskCategory() == RiskCategory.CRITICAL);
        assertTrue((Boolean) result.inferenceMetadata().get("synthetic"));
    }
}
