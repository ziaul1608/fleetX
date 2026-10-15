-- ==============================================================================
-- Module 5: Tracking & Control Tower Schema Enhancements
-- Reserved Range: V500 - V599
-- ==============================================================================

ALTER TABLE telemetry_logs ADD COLUMN simulated BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE telemetry_logs ADD COLUMN waypoint_label VARCHAR(120) NULL;

ALTER TABLE geofences ADD COLUMN shape_type VARCHAR(20) NOT NULL DEFAULT 'CIRCLE';
ALTER TABLE geofences ADD COLUMN polygon_coordinates_json TEXT NULL;
ALTER TABLE geofences ADD COLUMN speed_limit_kmh DECIMAL(6,2) NULL;
ALTER TABLE geofences ADD COLUMN created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6);

ALTER TABLE alerts ADD COLUMN acknowledged_by VARCHAR(180) NULL;
ALTER TABLE alerts ADD COLUMN acknowledged_at DATETIME(6) NULL;
ALTER TABLE alerts ADD COLUMN dedup_key VARCHAR(160) NULL;

CREATE INDEX idx_alerts_dedup ON alerts(alert_type, entity_type, entity_id, acknowledged);

CREATE TABLE IF NOT EXISTS geofence_vehicle_states (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    geofence_id BIGINT NOT NULL,
    vehicle_id BIGINT NOT NULL,
    inside BOOLEAN NOT NULL,
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_gvs_geofence FOREIGN KEY (geofence_id) REFERENCES geofences(id) ON DELETE CASCADE,
    CONSTRAINT uk_geofence_vehicle UNIQUE (geofence_id, vehicle_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
