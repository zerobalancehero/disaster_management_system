# ML Integration Contract & Specification

**System**: AI-Based Disaster Prediction & Emergency Response System  
**Feature Schema Version**: `v1.0`  
**Integration Interface**: `com.disaster.response.ml.DisasterRiskPredictor`  
**Status**: Ready for Trained Artifact Integration

---

## 1. Architectural Boundaries & Safety Rules

1. **Safety Separation**: AI risk predictions **NEVER** automatically verify incident reports or dispatch response teams. All dispatches require explicit human administrator approval.
2. **No Fabrication**: If the production model artifact is missing or fails, the system returns a controlled `ModelUnavailableException` (HTTP 503). It **never** fabricates fake production predictions.
3. **Synthetic Marking**: In `dev` profile with mock inference enabled, predictions are explicitly flagged with `modelVersion: "SYNTHETIC-MOCK-v1.0"` and `isSynthetic: true`.

---

## 2. Feature Schema (`v1.0`)

The inference engine consumes the typed, immutable feature vector `RiskFeatureVector`:

| Feature Name | Data Type | Units / Format | Valid Range | Required? | Source / Extraction Method |
|--------------|-----------|----------------|-------------|-----------|----------------------------|
| `regionId` | `UUID` | UUID | Valid UUID | Yes | Monitored region identifier |
| `disasterType` | `Enum` | `DisasterType` | `FLOOD`, `EARTHQUAKE`, `CYCLONE`, `LANDSLIDE`, `WILDFIRE`, `HAZMAT_SPILL`, `TSUNAMI` | Yes | Target disaster risk class |
| `predictionHorizon` | `Enum` | `PredictionHorizon` | `NEXT_6H`, `NEXT_24H`, `NEXT_48H`, `NEXT_7D` | Yes | Target forecast window |
| `temperatureCelsius` | `Double` | Celsius (°C) | `[-100.0, 80.0]` | No | Nearest weather observation / NWP forecast |
| `humidityPercent` | `Double` | Percent (%) | `[0.0, 100.0]` | No | Atmospheric relative humidity |
| `rainfallMm` | `Double` | Millimeters (mm) | `[0.0, 2000.0]` | No | Precipitation rate / cumulative forecast |
| `windSpeedKmh` | `Double` | Kilometers/hour (km/h) | `[0.0, 500.0]` | No | Wind velocity at 10m height |
| `elevationMeters` | `Double` | Meters above sea level | Any realistic | No | Extracted from DEM (SRTM / Copernicus) |
| `slopeDegrees` | `Double` | Degrees (°) | `[0.0, 90.0]` | No | Topographic gradient from DEM |
| `flowAccumulation` | `Double` | Upslope contributing cells | `>= 0.0` | No | Hydrological flow network accumulation |
| `distanceToRiverMeters`| `Double` | Meters (m) | `>= 0.0` | No | Proximity to nearest drainage river channel |

---

## 3. Risk Thresholds & Category Mapping

Predictions output a continuous calibrated probability $P \in [0.0, 1.0]$. The backend maps probabilities to categorical alerts using configurable thresholds:

| Continuous Probability | Risk Category | Operational Meaning |
|------------------------|---------------|---------------------|
| $P < 0.30$ | `LOW` | Normal baseline monitoring. No immediate public alert. |
| $0.30 \le P < 0.60$ | `MEDIUM` | Advisory issued. Preparedness checks for response teams. |
| $0.60 \le P < 0.85$ | `HIGH` | High-risk alert. Pre-positioning recommended. |
| $P \ge 0.85$ | `CRITICAL` | Imminent disaster risk. Multi-agency alert broadcast. |

---

## 4. Java-Native Integration Interface

```java
package com.disaster.response.ml;

public interface DisasterRiskPredictor {
    RiskPredictionResult predict(RiskFeatureVector features);
    boolean isAvailable();
    String getModelVersion();
    String getFramework();
}
```

### Result Record:
```java
public record RiskPredictionResult(
    UUID regionId,
    DisasterType disasterType,
    double probability,
    RiskCategory riskCategory,
    PredictionHorizon predictionHorizon,
    Instant timestamp,
    String modelVersion,
    String framework,
    Map<String, Object> featureSnapshot,
    Map<String, Object> inferenceMetadata
) {}
```

---

## 5. How to Plug In Your Trained Model Artifact

### Option A: ONNX Runtime (Recommended)
1. Export your trained model (PyTorch, Scikit-Learn, LightGBM, or XGBoost) to `.onnx`:
   ```python
   # Example Python export
   import skl2onnx
   onnx_model = skl2onnx.convert_sklearn(pipeline, "disaster_risk_model", initial_types)
   with open("disaster_risk_model.onnx", "wb") as f:
       f.write(onnx_model.SerializeToString())
   ```
2. Place the file in `src/main/resources/models/` or configure via `.env`:
   ```properties
   ML_MOCK_ENABLED=false
   ML_MODEL_PATH=/app/models/disaster_risk_model.onnx
   ```
3. In `ProductionModelPredictor.java`, initialize `OrtEnvironment` and `OrtSession`.

### Option B: Tribuo / Native Java
1. Save the trained Tribuo model using Java Serialization / Protobuf.
2. Load the model via `org.tribuo.Model.deserialize()`.
3. Provide the file path to `ProductionModelPredictor`.
