-- V1__init_schema.sql
-- AI-Based Disaster Prediction & Emergency Response System
-- PostgreSQL with PostGIS schema initialization
-- Coordinate Reference System: SRID 4326 (WGS 84), Coordinate Order: (Longitude, Latitude)

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS postgis;

-- 1. USERS
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    phone VARCHAR(50),
    role VARCHAR(50) NOT NULL, -- 'USER', 'RESPONDER', 'ADMIN'
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_role ON users(role);
CREATE INDEX idx_users_active ON users(active);

-- 2. RESPONDER TEAMS
CREATE TABLE responder_teams (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    team_type VARCHAR(100) NOT NULL, -- 'SEARCH_AND_RESCUE', 'MEDICAL', 'FIRE_RESCUE', 'EVACUATION', 'HAZMAT', etc.
    availability_status VARCHAR(50) NOT NULL DEFAULT 'AVAILABLE', -- 'AVAILABLE', 'DISPATCHED', 'DEPLOYED', 'OFF_DUTY', 'MAINTENANCE'
    base_location GEOMETRY(Point, 4326) NOT NULL,
    contact_details VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_responder_teams_status ON responder_teams(availability_status);
CREATE INDEX idx_responder_teams_base_loc ON responder_teams USING GIST(base_location);

-- 3. TEAM MEMBERS
CREATE TABLE team_members (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    team_id UUID NOT NULL REFERENCES responder_teams(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    membership_status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE', -- 'ACTIVE', 'INACTIVE'
    joined_date TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_team_user_active UNIQUE (team_id, user_id)
);

CREATE INDEX idx_team_members_team_id ON team_members(team_id);
CREATE INDEX idx_team_members_user_id ON team_members(user_id);
CREATE INDEX idx_team_members_status ON team_members(membership_status);

-- 4. TEAM CAPABILITIES
CREATE TABLE team_capabilities (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    team_id UUID NOT NULL REFERENCES responder_teams(id) ON DELETE CASCADE,
    capability VARCHAR(100) NOT NULL, -- e.g., 'WATER_RESCUE', 'BOAT_OPERATIONS', 'COLLAPSED_STRUCTURE', 'HIGH_ANGLE_RESCUE', 'FIRST_AID', 'WILDFIRE_SUPPRESSION'
    equipment_metadata TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_team_capabilities_team_id ON team_capabilities(team_id);
CREATE INDEX idx_team_capabilities_cap ON team_capabilities(capability);

-- 5. REGIONS (Admin boundaries & grid cells)
CREATE TABLE regions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    region_type VARCHAR(50) NOT NULL, -- 'DISTRICT', 'MUNICIPALITY', 'MODEL_GRID', 'COASTAL_ZONE'
    boundary GEOMETRY(Geometry, 4326) NOT NULL,
    center_point GEOMETRY(Point, 4326) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_regions_boundary ON regions USING GIST(boundary);
CREATE INDEX idx_regions_center ON regions USING GIST(center_point);
CREATE INDEX idx_regions_type ON regions(region_type);

-- 6. WEATHER OBSERVATIONS
CREATE TABLE weather_observations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    source VARCHAR(100) NOT NULL, -- 'OPEN_METEO', 'IMD', etc.
    external_location_id VARCHAR(100),
    coordinates GEOMETRY(Point, 4326) NOT NULL,
    observation_time TIMESTAMP WITH TIME ZONE NOT NULL,
    temperature DOUBLE PRECISION,
    humidity DOUBLE PRECISION,
    rainfall DOUBLE PRECISION,
    wind_speed DOUBLE PRECISION,
    weather_condition VARCHAR(100),
    raw_payload TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_weather_obs_coords ON weather_observations USING GIST(coordinates);
CREATE INDEX idx_weather_obs_time ON weather_observations(observation_time DESC);
CREATE INDEX idx_weather_obs_source ON weather_observations(source);

-- 7. WEATHER FORECASTS
CREATE TABLE weather_forecasts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    source VARCHAR(100) NOT NULL,
    coordinates GEOMETRY(Point, 4326) NOT NULL,
    region_id UUID REFERENCES regions(id) ON DELETE SET NULL,
    forecast_issue_time TIMESTAMP WITH TIME ZONE NOT NULL,
    forecast_target_time TIMESTAMP WITH TIME ZONE NOT NULL,
    temperature DOUBLE PRECISION,
    rainfall DOUBLE PRECISION,
    wind_speed DOUBLE PRECISION,
    weather_condition VARCHAR(100),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_weather_fc_coords ON weather_forecasts USING GIST(coordinates);
CREATE INDEX idx_weather_fc_target ON weather_forecasts(forecast_target_time);
CREATE INDEX idx_weather_fc_region ON weather_forecasts(region_id);

-- 8. DISASTER ALERTS
CREATE TABLE disaster_alerts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    source VARCHAR(100) NOT NULL, -- 'USGS', 'GDACS', 'IMD', 'SYSTEM'
    external_source_id VARCHAR(255) NOT NULL,
    disaster_type VARCHAR(100) NOT NULL, -- 'EARTHQUAKE', 'FLOOD', 'CYCLONE', 'LANDSLIDE', 'WILDFIRE'
    severity VARCHAR(50) NOT NULL, -- 'INFO', 'LOW', 'MODERATE', 'SEVERE', 'EXTREME'
    title VARCHAR(255) NOT NULL,
    description TEXT,
    affected_area GEOMETRY(Geometry, 4326) NOT NULL,
    issue_time TIMESTAMP WITH TIME ZONE NOT NULL,
    expiry_time TIMESTAMP WITH TIME ZONE,
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE', -- 'ACTIVE', 'EXPIRED', 'CANCELLED'
    raw_payload TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_alert_source_external_id UNIQUE (source, external_source_id)
);

CREATE INDEX idx_disaster_alerts_area ON disaster_alerts USING GIST(affected_area);
CREATE INDEX idx_disaster_alerts_status ON disaster_alerts(status);
CREATE INDEX idx_disaster_alerts_issue ON disaster_alerts(issue_time DESC);
CREATE INDEX idx_disaster_alerts_type ON disaster_alerts(disaster_type);

-- 9. TERRAIN FEATURES
CREATE TABLE terrain_features (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    region_id UUID NOT NULL REFERENCES regions(id) ON DELETE CASCADE,
    elevation DOUBLE PRECISION NOT NULL,
    slope DOUBLE PRECISION,
    flow_accumulation DOUBLE PRECISION,
    distance_to_river DOUBLE PRECISION,
    data_source VARCHAR(100) NOT NULL, -- 'OPENTOPOGRAPHY', 'COPERNICUS_DEM', 'NASA_SRTM'
    processing_time TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_terrain_features_region ON terrain_features(region_id);

-- 10. RISK ASSESSMENTS
CREATE TABLE risk_assessments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    region_id UUID NOT NULL REFERENCES regions(id) ON DELETE CASCADE,
    disaster_type VARCHAR(100) NOT NULL,
    probability DOUBLE PRECISION NOT NULL,
    risk_category VARCHAR(50) NOT NULL, -- 'LOW', 'MEDIUM', 'HIGH', 'CRITICAL'
    prediction_horizon VARCHAR(50) NOT NULL, -- 'NEXT_6H', 'NEXT_24H', 'NEXT_48H', 'NEXT_7D'
    prediction_timestamp TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    model_version VARCHAR(100) NOT NULL,
    feature_snapshot TEXT,
    inference_metadata TEXT
);

CREATE INDEX idx_risk_assessments_region ON risk_assessments(region_id);
CREATE INDEX idx_risk_assessments_type ON risk_assessments(disaster_type);
CREATE INDEX idx_risk_assessments_time ON risk_assessments(prediction_timestamp DESC);
CREATE INDEX idx_risk_assessments_cat ON risk_assessments(risk_category);

-- 11. INCIDENTS
CREATE TABLE incidents (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    reporter_id UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    title VARCHAR(255) NOT NULL,
    description TEXT NOT NULL,
    disaster_type VARCHAR(100) NOT NULL,
    severity VARCHAR(50) NOT NULL, -- 'LOW', 'MEDIUM', 'HIGH', 'CRITICAL'
    location GEOMETRY(Point, 4326) NOT NULL,
    reported_time TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    verification_status VARCHAR(50) NOT NULL DEFAULT 'PENDING', -- 'PENDING', 'VERIFIED', 'REJECTED'
    verifier_id UUID REFERENCES users(id) ON DELETE SET NULL,
    verification_time TIMESTAMP WITH TIME ZONE,
    rejection_reason TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_incidents_location ON incidents USING GIST(location);
CREATE INDEX idx_incidents_reporter ON incidents(reporter_id);
CREATE INDEX idx_incidents_status ON incidents(verification_status);
CREATE INDEX idx_incidents_time ON incidents(reported_time DESC);
CREATE INDEX idx_incidents_type ON incidents(disaster_type);

-- 12. ASSIGNMENT RECOMMENDATIONS
CREATE TABLE assignment_recommendations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    incident_id UUID NOT NULL REFERENCES incidents(id) ON DELETE CASCADE,
    team_id UUID NOT NULL REFERENCES responder_teams(id) ON DELETE CASCADE,
    rank INTEGER NOT NULL,
    estimated_travel_time_minutes INTEGER,
    eligibility BOOLEAN NOT NULL DEFAULT TRUE,
    availability_snapshot VARCHAR(50) NOT NULL,
    recommendation_reason TEXT NOT NULL,
    generation_time TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    recommendation_status VARCHAR(50) NOT NULL DEFAULT 'PROPOSED' -- 'PROPOSED', 'APPROVED', 'REJECTED', 'SUPERSEDED'
);

CREATE INDEX idx_recommendations_incident ON assignment_recommendations(incident_id);
CREATE INDEX idx_recommendations_team ON assignment_recommendations(team_id);
CREATE INDEX idx_recommendations_status ON assignment_recommendations(recommendation_status);

-- 13. INCIDENT ASSIGNMENTS
CREATE TABLE incident_assignments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    incident_id UUID NOT NULL REFERENCES incidents(id) ON DELETE RESTRICT,
    team_id UUID NOT NULL REFERENCES responder_teams(id) ON DELETE RESTRICT,
    approving_admin_id UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    approval_time TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    assignment_status VARCHAR(50) NOT NULL DEFAULT 'PENDING_APPROVAL', -- 'PENDING_APPROVAL', 'APPROVED', 'REJECTED', 'ACCEPTED', 'EN_ROUTE', 'ON_SCENE', 'RESOLVED', 'CANCELLED'
    assignment_time TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completion_time TIMESTAMP WITH TIME ZONE,
    rejection_or_cancellation_reason TEXT,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_assignments_incident ON incident_assignments(incident_id);
CREATE INDEX idx_assignments_team ON incident_assignments(team_id);
CREATE INDEX idx_assignments_status ON incident_assignments(assignment_status);
CREATE INDEX idx_assignments_admin ON incident_assignments(approving_admin_id);

-- 14. RESPONSE UPDATES
CREATE TABLE response_updates (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    assignment_id UUID NOT NULL REFERENCES incident_assignments(id) ON DELETE CASCADE,
    actor_id UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    status VARCHAR(50) NOT NULL,
    message TEXT NOT NULL,
    location GEOMETRY(Point, 4326),
    timestamp TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_response_updates_assignment ON response_updates(assignment_id);
CREATE INDEX idx_response_updates_time ON response_updates(timestamp DESC);
CREATE INDEX idx_response_updates_actor ON response_updates(actor_id);

-- 15. SAFETY INSTRUCTIONS
CREATE TABLE safety_instructions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title VARCHAR(255) NOT NULL,
    disaster_type VARCHAR(100) NOT NULL,
    minimum_severity VARCHAR(50) NOT NULL DEFAULT 'LOW',
    instruction_text TEXT NOT NULL,
    target_audience VARCHAR(50) NOT NULL DEFAULT 'GENERAL_PUBLIC', -- 'GENERAL_PUBLIC', 'FIRST_RESPONDERS', 'EVACUEES'
    language VARCHAR(10) NOT NULL DEFAULT 'en',
    active BOOLEAN NOT NULL DEFAULT TRUE,
    creator_id UUID REFERENCES users(id) ON DELETE SET NULL,
    updater_id UUID REFERENCES users(id) ON DELETE SET NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_safety_instructions_type ON safety_instructions(disaster_type);
CREATE INDEX idx_safety_instructions_active ON safety_instructions(active);
CREATE INDEX idx_safety_instructions_lang ON safety_instructions(language);

-- 16. AUDIT LOGS (Immutable append-only)
CREATE TABLE audit_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    actor_id UUID REFERENCES users(id) ON DELETE SET NULL,
    action VARCHAR(100) NOT NULL,
    entity_type VARCHAR(100) NOT NULL,
    entity_id VARCHAR(255) NOT NULL,
    old_values TEXT,
    new_values TEXT,
    timestamp TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_audit_logs_actor ON audit_logs(actor_id);
CREATE INDEX idx_audit_logs_entity ON audit_logs(entity_type, entity_id);
CREATE INDEX idx_audit_logs_action ON audit_logs(action);
CREATE INDEX idx_audit_logs_time ON audit_logs(timestamp DESC);

-- 17. NOTIFICATIONS
CREATE TABLE notifications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    recipient_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    type VARCHAR(100) NOT NULL, -- 'DISPATCH_ALERT', 'STATUS_CHANGE', 'ASSIGNMENT_UPDATE', 'ALERT_BROADCAST'
    title VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    related_entity_type VARCHAR(100),
    related_entity_id VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    read_at TIMESTAMP WITH TIME ZONE,
    delivery_status VARCHAR(50) NOT NULL DEFAULT 'DELIVERED' -- 'DELIVERED', 'FAILED', 'PENDING'
);

CREATE INDEX idx_notifications_recipient ON notifications(recipient_id);
CREATE INDEX idx_notifications_read ON notifications(read_at);
CREATE INDEX idx_notifications_time ON notifications(created_at DESC);

-- 18. DATA SOURCE RUNS
CREATE TABLE data_source_runs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    provider VARCHAR(100) NOT NULL,
    job_type VARCHAR(100) NOT NULL,
    start_time TIMESTAMP WITH TIME ZONE NOT NULL,
    end_time TIMESTAMP WITH TIME ZONE,
    status VARCHAR(50) NOT NULL, -- 'RUNNING', 'SUCCESS', 'FAILURE', 'PARTIAL'
    records_processed INTEGER NOT NULL DEFAULT 0,
    error_summary TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_data_source_runs_provider ON data_source_runs(provider);
CREATE INDEX idx_data_source_runs_status ON data_source_runs(status);
CREATE INDEX idx_data_source_runs_start ON data_source_runs(start_time DESC);

-- 19. MODEL REGISTRY
CREATE TABLE model_registry (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    model_name VARCHAR(100) NOT NULL,
    version VARCHAR(50) NOT NULL,
    artifact_location VARCHAR(500) NOT NULL,
    framework VARCHAR(100) NOT NULL, -- 'ONNX', 'TRIBUO', 'NATIVE_JAVA', 'DETERMINISTIC_MOCK'
    active BOOLEAN NOT NULL DEFAULT FALSE,
    feature_schema_version VARCHAR(50) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    evaluation_metadata TEXT,
    CONSTRAINT uk_model_name_version UNIQUE (model_name, version)
);

CREATE INDEX idx_model_registry_active ON model_registry(active);
