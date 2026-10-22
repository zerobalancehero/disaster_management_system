# AI-Based Disaster Prediction & Emergency Response System

Production-grade, enterprise backend for disaster alerts, weather tracking, DEM & GIS spatial processing, AI-driven disaster risk prediction, and human-in-the-loop emergency team dispatching.

---

## 1. System Architecture & Capabilities

```
+-------------------------------------------------------------------------------------------------+
|                                        SPRING BOOT 3.3.4 (JAVA 17/21)                           |
|                                                                                                 |
|   +-------------------+   +-------------------------+   +-------------------+                   |
|   |  Security / JWT   |   |   Emergency Dispatch    |   |    GIS / DEM      |                   |
|   |  Role-Based Auth  |   |   State Machine         |   |  Spatial Analysis |                   |
|   |  (USER/RESP/ADMIN)|   | (Approved->OnScene->Res)|   |  (SRID 4326, JTS) |                   |
|   +-------------------+   +-------------------------+   +-------------------+                   |
|                                                                                                 |
|   +-------------------+   +-------------------------+   +-------------------+                   |
|   |   ML Inference    |   |   External Ingestion    |   | Audit & Reporting |                   |
|   |   Predictor API   |   |  (Open-Meteo, USGS,     |   | Immutable Logs &  |                   |
|   |   (Safe / Mock)   |   |   GDACS, IMD Adapter)   |   | Performance KPIs  |                   |
|   +-------------------+   +-------------------------+   +-------------------+                   |
+-------------------------------------------------------------------------------------------------+
                                      |
                                      v
                 +----------------------------------------+
                 |       PostgreSQL 16 with PostGIS       |
                 |      Flyway Database Migrations        |
                 |       19 Production Relational         |
                 |           & Spatial Tables             |
                 +----------------------------------------+
```

### Core Workflows & Safety Invariants
1. **Rule of Verification**: Citizen incident reports start in `PENDING` state and **must** be verified by an authorized `ADMIN` before becoming eligible for recommendations.
2. **Rule of Human-in-the-Loop Dispatch**: AI risk predictions and automated recommendations **never** dispatch teams automatically. An authorized administrator must explicitly approve a recommendation to dispatch a team.
3. **Strict State Machine**: Assignment progression follows:
   `PENDING_APPROVAL` -> `APPROVED` -> `ACCEPTED` -> `EN_ROUTE` -> `ON_SCENE` -> `RESOLVED` (or `CANCELLED` / `REJECTED`).
4. **Data Honesty**: Stale weather data is explicitly flagged (`isStale: true`) and never represented as live data. Missing ML model artifacts return controlled 503 errors and never fabricate fake predictions.
5. **Role-Based Server Enforcement**: Responders can only access assignments belonging strictly to their assigned team(s).

---

## 2. Technology Stack

- **Java**: 17 / 21
- **Framework**: Spring Boot 3.3.4
- **Database**: PostgreSQL 16 with PostGIS 3.4
- **Spatial GIS Engine**: Hibernate Spatial, LocationTech JTS Core 1.19
- **Database Migrations**: Flyway Core & Flyway PostgreSQL
- **Security**: Spring Security 6 with JJWT 0.12.6 (stateless Bearer JWT)
- **API Documentation**: SpringDoc OpenAPI 2.6.0 (Swagger UI)
- **Containerization**: Docker multi-stage build & Docker Compose
- **Testing**: JUnit 5, Mockito, Spring Security Test

---

## 3. Quick Start & Running Locally

### Prerequisites
- JDK 17 or JDK 21 installed (`java -version`)
- Apache Maven 3.9+ (`mvn -v`)
- PostgreSQL with PostGIS installed locally, OR Docker & Docker Compose

### Option A: Running with Docker Compose (Recommended for Full Stack)
1. Copy the configuration file:
   ```bash
   cp .env.example .env
   ```
2. Start the database and backend:
   ```bash
   docker compose up --build
   ```
3. Access Swagger UI at `http://localhost:8080/swagger-ui.html`.

### Option B: Running with Maven and Local PostgreSQL
1. Ensure your local PostgreSQL has the PostGIS extension:
   ```sql
   CREATE DATABASE disaster_db;
   \c disaster_db
   CREATE EXTENSION postgis;
   ```
2. Configure credentials in `.env` or set environment variables:
   ```powershell
   $env:DB_URL="jdbc:postgresql://localhost:5432/disaster_db"
   $env:DB_USERNAME="postgres"
   $env:DB_PASSWORD="your_password"
   $env:SPRING_PROFILES_ACTIVE="dev"
   $env:SEED_DEV_DATA="true"
   $env:ML_MOCK_ENABLED="true"
   ```
3. Run the application:
   ```bash
   mvn spring-boot:run
   ```

---

## 4. Development Seed Accounts

When running with `SPRING_PROFILES_ACTIVE=dev` or `SEED_DEV_DATA=true`, the following accounts and data are seeded automatically:

| Role | Email | Password | Assigned Permissions / Capabilities |
|------|-------|----------|-------------------------------------|
| **ADMIN** | `admin@disaster.local` | `Admin@123` | System-wide incident verification, dispatch approval/rejection, team & user management, safety instructions, audit logs, performance reporting |
| **RESPONDER** | `responder@disaster.local` | `Responder@123` | Assigned to **Alpha Water Rescue Squad**. Can view assigned dispatches, update status (`ACCEPTED` -> `EN_ROUTE` -> `ON_SCENE` -> `RESOLVED`), submit field updates |
| **USER** | `user@disaster.local` | `User@123` | Submit disaster incident reports, view status of own reports, view active weather alerts and safety instructions |

---

## 5. Running Automated Tests

Run the complete test suite:
```bash
mvn clean test
```

### Verified Test Suites:
- `AuthAndSecurityTest`: Authentication success, password hashing, invalid credentials, deactivated accounts, JWT issuance and validation.
- `RoleBasedAccessSecurityTest`: Responders cannot read or modify other teams' assignments (Rule 7 enforcement).
- `IncidentVerificationWorkflowTest`: Incident verification (`PENDING` -> `VERIFIED`) and rejection (`PENDING` -> `REJECTED`) with audit logging.
- `EmergencyDispatchEngineTest`:
  - Unverified incidents cannot receive recommendations (Rule 3).
  - Recommendations do not dispatch teams (Rule 4).
  - Explicit admin approval required for dispatch (Rule 5).
  - Rejected recommendations never dispatch (Rule 6).
  - Double approval prevention and idempotency (Rule 11).
  - State machine transition enforcement (Rule 9).
- `ExternalAlertIngestionTest`: Deduplication of external alerts by `(source, externalSourceId)` and `DataSourceRun` tracking.
- `WeatherAndStaleDataTest`: Honest stale weather reporting (`isStale: true`) and non-fabrication.
- `MlInferenceAndFeatureValidationTest`: Feature validation, missing model safe failure, and mock predictor.
- `GisAndSpatialCalculationTest`: Coordinate boundaries, SRID 4326 JTS (Lon, Lat) ordering, Haversine distances, and buffer containment.

---

## 6. Environment Variables Reference

| Variable | Default Value | Description |
|----------|---------------|-------------|
| `PORT` | `8080` | HTTP port |
| `SPRING_PROFILES_ACTIVE` | `dev` | Active Spring profile (`dev`, `prod`, `test`) |
| `DB_URL` | `jdbc:postgresql://localhost:5432/disaster_db` | JDBC connection URL |
| `DB_USERNAME` | `postgres` | Database user |
| `DB_PASSWORD` | `postgres` | Database password |
| `JWT_SECRET` | 256-bit Hex Key | Secret key for HMAC-SHA signing |
| `JWT_EXPIRATION_MS` | `86400000` (24h) | JWT validity duration |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:3000,http://localhost:5173` | Allowed React frontend origins |
| `SEED_DEV_DATA` | `true` (in dev) | Whether to seed mock users and teams |
| `ML_MOCK_ENABLED` | `true` (in dev) | Enable deterministic synthetic predictor |
| `ML_MODEL_PATH` | `/app/models/model.onnx` | Path to production trained model artifact |
| `OPEN_METEO_ENABLED` | `true` | Open-Meteo weather API integration |
| `USGS_ENABLED` | `true` | USGS earthquake API integration |
| `OSRM_ENABLED` | `false` | Optional road routing engine |

---

## 7. Documentation Links

- **REST API Contract**: [docs/api-contract.md](docs/api-contract.md)
- **ML Integration Contract**: [docs/ml-integration.md](docs/ml-integration.md)
- **Interactive Swagger UI**: `http://localhost:8080/swagger-ui.html`
- **OpenAPI JSON Spec**: `http://localhost:8080/v3/api-docs`
