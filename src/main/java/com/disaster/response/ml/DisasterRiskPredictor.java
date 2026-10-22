package com.disaster.response.ml;

public interface DisasterRiskPredictor {

    /**
     * Executes risk prediction for given features.
     * @param features Typed, validated feature vector
     * @return RiskPredictionResult containing probability, category, and metadata
     */
    RiskPredictionResult predict(RiskFeatureVector features);

    /**
     * Returns true if the predictor is currently available and ready to serve inference.
     */
    boolean isAvailable();

    /**
     * Returns the model identifier or version.
     */
    String getModelVersion();

    /**
     * Returns the framework name (e.g. ONNX, NATIVE_JAVA, DETERMINISTIC_MOCK).
     */
    String getFramework();
}
