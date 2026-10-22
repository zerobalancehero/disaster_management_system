# REST API Contract & Specification

**Base URL**: `/api/v1`  
**Authentication**: Bearer JWT token in header: `Authorization: Bearer <token>`  
**Standard Coordinate Format**: WGS 84 (SRID 4326), Latitude `[-90.0, 90.0]`, Longitude `[-180.0, 180.0]`  
**Standard Date/Time Format**: ISO 8601 UTC (`yyyy-MM-dd'T'HH:mm:ss'Z'`)

---

## 1. Authentication & Users

### `POST /api/v1/auth/login`
- **Role**: Public
- **Description**: Authenticate with email and password to receive a JWT access token.
- **Request Body**:
```json
{
  "email": "user@disaster.local",
  "password": "User@123"
}
```
- **Response** (`200 OK`):
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiI1NTBlODQwMC1lMjliLTRkNDQtYjc2ZC00MjY2MTQxNzQwMDAiLCJlbWFpbCI6InVzZXJAZGlzYXN0ZXIubG9jYWwiLCJyb2xlIjoiVVNFUiIsImlhdCI6MTcyOTU3NDAwMCwiZXhwIjoxNzI5NjYwNDAwfQ.example_signature",
  "tokenType": "Bearer",
  "expiresInMs": 86400000,
  "user": {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "name": "Ananya Sharma",
    "email": "user@disaster.local",
    "phone": "+91-9876543212",
    "role": "USER",
    "active": true,
    "createdAt": "2026-10-22T04:30:00Z"
  }
}
```

### `GET /api/v1/users/me`
- **Role**: `USER`, `RESPONDER`, `ADMIN`
- **Description**: Get profile information for currently authenticated user.
- **Response** (`200 OK`):
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "name": "Ananya Sharma",
  "email": "user@disaster.local",
  "phone": "+91-9876543212",
  "role": "USER",
  "active": true,
  "createdAt": "2026-10-22T04:30:00Z"
}
```

---

## 2. Weather & Forecasts

### `GET /api/v1/weather/current?lat={lat}&lon={lon}`
- **Role**: `USER`, `RESPONDER`, `ADMIN`
- **Description**: Returns live or cached weather observations with honest freshness tracking.
- **Parameters**: `lat` (double), `lon` (double)
- **Response** (`200 OK`):
```json
{
  "source": "OPEN_METEO",
  "location": {
    "latitude": 13.0827,
    "longitude": 80.2707
  },
  "observationTime": "2026-10-22T04:45:00Z",
  "temperatureCelsius": 29.4,
  "humidityPercent": 84.0,
  "rainfallMm": 78.5,
  "windSpeedKmh": 42.0,
  "weatherCondition": "HEAVY_RAIN",
  "isStale": false,
  "freshnessNote": "Live data"
}
```

### `GET /api/v1/weather/forecast?lat={lat}&lon={lon}`
- **Role**: `USER`, `RESPONDER`, `ADMIN`
- **Description**: Returns 2-day hourly/interval forecasts.
- **Response** (`200 OK`):
```json
[
  {
    "id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
    "source": "OPEN_METEO",
    "coordinates": {
      "latitude": 13.0827,
      "longitude": 80.2707
    },
    "regionId": "9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d",
    "forecastIssueTime": "2026-10-22T04:00:00Z",
    "forecastTargetTime": "2026-10-22T07:00:00Z",
    "temperatureCelsius": 28.0,
    "rainfallMm": 55.0,
    "windSpeedKmh": 48.0,
    "weatherCondition": "THUNDERSTORM"
  }
]
```

---

## 3. Disaster Alerts

### `GET /api/v1/alerts/active`
- **Role**: `USER`, `RESPONDER`, `ADMIN`
- **Description**: Returns currently active, non-expired disaster warnings.
- **Response** (`200 OK`):
```json
[
  {
    "id": "e2a1b3c4-d5e6-4f7a-8b9c-0d1e2f3a4b5c",
    "source": "USGS",
    "externalSourceId": "us7000m9ab",
    "disasterType": "EARTHQUAKE",
    "severity": "SEVERE",
    "title": "M 6.4 - Bay of Bengal Region",
    "description": "Magnitude 6.4 earthquake detected at coordinates (13.2000, 80.4000)",
    "issueTime": "2026-10-22T03:15:00Z",
    "expiryTime": "2026-10-24T03:15:00Z",
    "status": "ACTIVE",
    "affectedAreaWkt": "POLYGON((80.9 13.2, 80.4 13.7, 79.9 13.2, 80.4 12.7, 80.9 13.2))"
  }
]
```

### `GET /api/v1/alerts/history?page=0&size=20`
- **Role**: `USER`, `RESPONDER`, `ADMIN`
- **Description**: Paginated archive of all historical alerts.

### `GET /api/v1/alerts/{id}/safety-instructions?lang=en`
- **Role**: `USER`, `RESPONDER`, `ADMIN`
- **Description**: Returns safety instructions tailored to the alert's disaster type and severity.
- **Response** (`200 OK`):
```json
[
  {
    "id": "f1a2b3c4-1111-2222-3333-444455556666",
    "title": "Earthquake Drop, Cover, and Hold Protocol",
    "disasterType": "EARTHQUAKE",
    "minimumSeverity": "LOW",
    "instructionText": "1. Drop onto your hands and knees. 2. Cover your head and neck under a sturdy table or desk. 3. Hold on until shaking stops.",
    "targetAudience": "GENERAL_PUBLIC",
    "language": "en",
    "active": true,
    "updatedAt": "2026-10-22T04:30:00Z"
  }
]
```

---

## 4. Incidents & Citizen Reporting

### `POST /api/v1/incidents/reports`
- **Role**: `USER`, `RESPONDER`, `ADMIN`
- **Description**: Submit a new incident report. Default verification status is `PENDING`.
- **Request Body**:
```json
{
  "title": "Flash flooding in residential apartment basement",
  "description": "Water level reaching 4 feet; power transformer submerged. 12 residents trapped.",
  "disasterType": "FLOOD",
  "severity": "HIGH",
  "location": {
    "latitude": 13.0600,
    "longitude": 80.2400
  }
}
```
- **Response** (`201 Created`):
```json
{
  "id": "c1d2e3f4-a1b2-c3d4-e5f6-a7b8c9d0e1f2",
  "reporterId": "550e8400-e29b-41d4-a716-446655440000",
  "reporterName": "Ananya Sharma",
  "title": "Flash flooding in residential apartment basement",
  "description": "Water level reaching 4 feet; power transformer submerged. 12 residents trapped.",
  "disasterType": "FLOOD",
  "severity": "HIGH",
  "location": {
    "latitude": 13.06,
    "longitude": 80.24
  },
  "reportedTime": "2026-10-22T04:40:00Z",
  "verificationStatus": "PENDING",
  "verifierId": null,
  "verifierName": null,
  "verificationTime": null,
  "rejectionReason": null,
  "createdAt": "2026-10-22T04:40:00Z",
  "updatedAt": "2026-10-22T04:40:00Z"
}
```

### `GET /api/v1/incidents/my-reports`
- **Role**: `USER`, `RESPONDER`, `ADMIN`
- **Description**: Returns all reports submitted by the logged-in user without administrative/responder private notes.

---

## 5. Responder Operations

### `GET /api/v1/responder/my-assignments`
- **Role**: `RESPONDER`, `ADMIN`
- **Description**: Returns assignments assigned to the responder's authorized team(s).
- **Response** (`200 OK`):
```json
{
  "content": [
    {
      "id": "a9b8c7d6-e5f4-3a2b-1c0d-e9f8a7b6c5d4",
      "incidentId": "c1d2e3f4-a1b2-c3d4-e5f6-a7b8c9d0e1f2",
      "incidentTitle": "Flash flooding in residential apartment basement",
      "incidentLocation": {
        "latitude": 13.06,
        "longitude": 80.24
      },
      "teamId": "11112222-3333-4444-5555-666677778888",
      "teamName": "Alpha Water Rescue Squad",
      "approvingAdminId": "99998888-7777-6666-5555-444433332222",
      "approvingAdminName": "System Admin",
      "approvalTime": "2026-10-22T04:50:00Z",
      "assignmentStatus": "APPROVED",
      "assignmentTime": "2026-10-22T04:50:00Z",
      "completionTime": null,
      "rejectionOrCancellationReason": null,
      "version": 0,
      "updates": []
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 1,
  "totalPages": 1,
  "last": true
}
```

### `PATCH /api/v1/responder/my-assignments/{id}/status`
- **Role**: `RESPONDER`, `ADMIN`
- **Description**: Transition assignment through the state machine:
  `APPROVED` -> `ACCEPTED` -> `EN_ROUTE` -> `ON_SCENE` -> `RESOLVED`.
- **Request Body**:
```json
{
  "status": "ACCEPTED",
  "message": "Team Alpha acknowledges assignment and assembling gear",
  "location": {
    "latitude": 13.0827,
    "longitude": 80.2707
  }
}
```

---

## 6. Admin Workflows & Dispatch

### `PATCH /api/v1/admin/incidents/{id}/verify`
- **Role**: `ADMIN`
- **Description**: Verifies an incident, making it eligible for recommendations.
- **Request Body**:
```json
{
  "severityOverride": "HIGH",
  "comments": "Confirmed emergency by municipal CCTV inspection"
}
```

### `POST /api/v1/admin/incidents/{id}/recommendations`
- **Role**: `ADMIN`
- **Description**: Runs recommendation engine to generate ranked, explainable team proposals.
- **Response** (`200 OK`):
```json
[
  {
    "id": "77776666-5555-4444-3333-222211110000",
    "incidentId": "c1d2e3f4-a1b2-c3d4-e5f6-a7b8c9d0e1f2",
    "teamId": "11112222-3333-4444-5555-666677778888",
    "teamName": "Alpha Water Rescue Squad",
    "rank": 1,
    "estimatedTravelTimeMinutes": 18,
    "eligibility": true,
    "availabilitySnapshot": "AVAILABLE",
    "recommendationReason": "Matches required disaster capability. Estimated distance: 4.8 km (18 min via HAVERSINE_ESTIMATE). Team is currently AVAILABLE.",
    "generationTime": "2026-10-22T04:48:00Z",
    "recommendationStatus": "PROPOSED"
  }
]
```

### `POST /api/v1/admin/assignments/{recommendationId}/approve`
- **Role**: `ADMIN`
- **Description**: Atomically confirms dispatch of the recommended team.
- **Response** (`200 OK`):
```json
{
  "id": "a9b8c7d6-e5f4-3a2b-1c0d-e9f8a7b6c5d4",
  "incidentId": "c1d2e3f4-a1b2-c3d4-e5f6-a7b8c9d0e1f2",
  "incidentTitle": "Flash flooding in residential apartment basement",
  "teamId": "11112222-3333-4444-5555-666677778888",
  "teamName": "Alpha Water Rescue Squad",
  "approvingAdminName": "System Admin",
  "approvalTime": "2026-10-22T04:50:00Z",
  "assignmentStatus": "APPROVED",
  "version": 0
}
```

### `POST /api/v1/admin/assignments/{recommendationId}/reject`
- **Role**: `ADMIN`
- **Description**: Rejects a recommendation. Guaranteed to never dispatch a team.
- **Request Body**:
```json
{
  "rejectionReason": "Team Alpha scheduled for primary coastal zone standby"
}
```

---

## 7. Standard Error Response

```json
{
  "timestamp": "2026-10-22T10:00:00Z",
  "status": 400,
  "code": "VALIDATION_ERROR",
  "message": "The request contains invalid fields",
  "path": "/api/v1/incidents/reports",
  "details": {
    "title": "Title is required",
    "location.latitude": "Latitude must be <= 90.0"
  }
}
```
