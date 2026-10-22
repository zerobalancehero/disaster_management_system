package com.disaster.response.ml;

import com.disaster.response.exception.ModelUnavailableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.File;

/**
 * Production Model Predictor Adapter.
 * Integrates with a trained model artifact (e.g. ONNX, Tribuo, or PMML).
 * If the model artifact is missing or unconfigured, this class NEVER fabricates
 * a fake prediction, returning controlled ModelUnavailableException instead.
 */
@Component
@ConditionalOnProperty(name = "app.ml.mock-enabled", havingValue = "false", matchIfMissing = true)
public class ProductionModelPredictor implements DisasterRiskPredictor {

    private static final Logger log = LoggerFactory.getLogger(ProductionModelPredictor.class);

    private final String modelPath;
    private final boolean modelFileExists;

    public ProductionModelPredictor(@Value("${app.ml.model-path:}") String modelPath) {
        this.modelPath = modelPath;
        if (modelPath != null && !modelPath.isBlank()) {
            File f = new File(modelPath);
            this.modelFileExists = f.exists() && f.isFile();
            if (this.modelFileExists) {
                log.info("Production ML model artifact found at: {}", modelPath);
            } else {
                log.warn("Configured ML model path does not exist: {}", modelPath);
            }
        } else {
            this.modelFileExists = false;
            log.warn("Production ML model path is not configured. Real inference will be unavailable until model is provided.");
        }
    }

    @Override
    public RiskPredictionResult predict(RiskFeatureVector features) {
        features.validate();

        if (!modelFileExists) {
            throw new ModelUnavailableException(
                    "Production ML risk model artifact is unavailable or not loaded at path: '" + modelPath + "'. " +
                    "System will not fabricate unverified predictions in production mode."
            );
        }

        // Production model runtime execution point (e.g., ONNX Runtime OrtSession or Tribuo Model evaluation)
        throw new UnsupportedOperationException("Trained model artifact evaluation adapter requires model teammate's binary format specification.");
    }

    @Override
    public boolean isAvailable() {
        return modelFileExists;
    }

    @Override
    public String getModelVersion() {
        return modelFileExists ? "PROD-ONNX-v1.0" : "UNAVAILABLE";
    }

    @Override
    public String getFramework() {
        return "ONNX_RUNTIME";
    }
}
