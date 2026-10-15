-- ==============================================================================
-- Module 5: Tracking & Control Tower Schema Baseline
-- Reserved Range: V500 - V599
-- ==============================================================================

CREATE TABLE IF NOT EXISTS telemetry_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    vehicle_id BIGINT NOT NULL,
    trip_id BIGINT NULL,
    latitude DECIMAL(10,7) NOT NULL,
    longitude DECIMAL(10,7) NOT NULL,
    speed_kmh DECIMAL(6,2) NOT NULL DEFAULT 0.00,
    heading DECIMAL(6,2) NOT NULL DEFAULT 0.00,
    current_odometer_km DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    recorded_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_telemetry_vehicle FOREIGN KEY (vehicle_id) REFERENCES vehicles(id) ON DELETE CASCADE,
    CONSTRAINT fk_telemetry_trip FOREIGN KEY (trip_id) REFERENCES trips(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE INDEX idx_telemetry_vehicle_time ON telemetry_logs(vehicle_id, recorded_at);
CREATE INDEX idx_telemetry_trip ON telemetry_logs(trip_id);

CREATE TABLE IF NOT EXISTS geofences (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    center_latitude DECIMAL(10,7) NOT NULL,
    center_longitude DECIMAL(10,7) NOT NULL,
    radius_meters DECIMAL(8,2) NOT NULL,
    fence_type VARCHAR(30) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS alerts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    alert_type VARCHAR(50) NOT NULL,
    severity VARCHAR(20) NOT NULL DEFAULT 'WARNING',
    entity_type VARCHAR(40) NOT NULL,
    entity_id BIGINT NOT NULL,
    geofence_id BIGINT NULL,
    message TEXT NOT NULL,
    acknowledged BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_alerts_geofence FOREIGN KEY (geofence_id) REFERENCES geofences(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE INDEX idx_alerts_unack ON alerts(acknowledged, created_at);

CREATE TABLE IF NOT EXISTS control_tower_kpis (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    snapshot_date DATE NOT NULL UNIQUE,
    active_vehicles INT NOT NULL DEFAULT 0,
    ongoing_trips INT NOT NULL DEFAULT 0,
    total_alerts INT NOT NULL DEFAULT 0,
    average_fuel_efficiency DECIMAL(6,2) NOT NULL DEFAULT 0.00,
    generated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
