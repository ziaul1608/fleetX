-- ==============================================================================
-- Module 6: Multi-Tenant SaaS & Email OTP Authentication Schema
-- Version: V600
-- ==============================================================================

-- 1. Create Tenants Table (Private Warehouse Workspaces)
CREATE TABLE IF NOT EXISTS tenants (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_code VARCHAR(50) NOT NULL UNIQUE,
    company_name VARCHAR(150) NOT NULL,
    warehouse_name VARCHAR(150) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE INDEX idx_tenants_status ON tenants(status);

-- 2. Seed Legacy Designated Tenant (Preserves pre-migration operational data under ID 1)
INSERT INTO tenants (id, tenant_code, company_name, warehouse_name, status, created_at, updated_at)
VALUES (1, 'TENANT-LEGACY-DEFAULT', 'FleetX Global Logistics (Legacy)', 'Central Warehouse Hub', 'ACTIVE', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6))
ON DUPLICATE KEY UPDATE company_name=company_name;

-- 3. Create Email OTP Challenge Table
CREATE TABLE IF NOT EXISTS otp_challenges (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(180) NOT NULL,
    otp_hash VARCHAR(255) NOT NULL,
    purpose VARCHAR(30) NOT NULL,
    attempts INT NOT NULL DEFAULT 0,
    max_attempts INT NOT NULL DEFAULT 5,
    resend_available_at DATETIME(6) NOT NULL,
    expires_at DATETIME(6) NOT NULL,
    verified BOOLEAN NOT NULL DEFAULT FALSE,
    verified_at DATETIME(6) NULL,
    metadata_json TEXT NULL,
    ip_address VARCHAR(45) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE INDEX idx_otp_email_purpose ON otp_challenges(email, purpose);
CREATE INDEX idx_otp_expires ON otp_challenges(expires_at);

-- 4. Scope Module 1 (Identity & Admin)
ALTER TABLE users ADD COLUMN tenant_id BIGINT NULL;
UPDATE users SET tenant_id = 1 WHERE tenant_id IS NULL;
ALTER TABLE users MODIFY COLUMN tenant_id BIGINT NOT NULL DEFAULT 1;
ALTER TABLE users MODIFY COLUMN password_hash VARCHAR(255) NULL;
ALTER TABLE users ADD CONSTRAINT fk_users_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE;
CREATE INDEX idx_users_tenant ON users(tenant_id);

ALTER TABLE audit_logs ADD COLUMN tenant_id BIGINT NULL;
UPDATE audit_logs SET tenant_id = 1 WHERE tenant_id IS NULL;
ALTER TABLE audit_logs MODIFY COLUMN tenant_id BIGINT NOT NULL DEFAULT 1;
ALTER TABLE audit_logs ADD CONSTRAINT fk_audit_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE;
CREATE INDEX idx_audit_tenant ON audit_logs(tenant_id);

-- 5. Scope Module 2 (Fleet & Assets)
ALTER TABLE vehicles ADD COLUMN tenant_id BIGINT NULL;
UPDATE vehicles SET tenant_id = 1 WHERE tenant_id IS NULL;
ALTER TABLE vehicles MODIFY COLUMN tenant_id BIGINT NOT NULL DEFAULT 1;
ALTER TABLE vehicles ADD CONSTRAINT fk_vehicles_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE;
CREATE INDEX idx_vehicles_tenant ON vehicles(tenant_id);
CREATE INDEX idx_vehicles_tenant_status ON vehicles(tenant_id, status);

ALTER TABLE fuel_records ADD COLUMN tenant_id BIGINT NULL;
UPDATE fuel_records SET tenant_id = 1 WHERE tenant_id IS NULL;
ALTER TABLE fuel_records MODIFY COLUMN tenant_id BIGINT NOT NULL DEFAULT 1;
ALTER TABLE fuel_records ADD CONSTRAINT fk_fuel_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE;
CREATE INDEX idx_fuel_tenant ON fuel_records(tenant_id);

ALTER TABLE maintenance_records ADD COLUMN tenant_id BIGINT NULL;
UPDATE maintenance_records SET tenant_id = 1 WHERE tenant_id IS NULL;
ALTER TABLE maintenance_records MODIFY COLUMN tenant_id BIGINT NOT NULL DEFAULT 1;
ALTER TABLE maintenance_records ADD CONSTRAINT fk_maint_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE;
CREATE INDEX idx_maint_tenant ON maintenance_records(tenant_id);

ALTER TABLE service_warnings ADD COLUMN tenant_id BIGINT NULL;
UPDATE service_warnings SET tenant_id = 1 WHERE tenant_id IS NULL;
ALTER TABLE service_warnings MODIFY COLUMN tenant_id BIGINT NOT NULL DEFAULT 1;
ALTER TABLE service_warnings ADD CONSTRAINT fk_warnings_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE;
CREATE INDEX idx_warnings_tenant ON service_warnings(tenant_id);

-- 6. Scope Module 3 (Drivers & Trips)
ALTER TABLE drivers ADD COLUMN tenant_id BIGINT NULL;
UPDATE drivers SET tenant_id = 1 WHERE tenant_id IS NULL;
ALTER TABLE drivers MODIFY COLUMN tenant_id BIGINT NOT NULL DEFAULT 1;
ALTER TABLE drivers ADD CONSTRAINT fk_drivers_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE;
CREATE INDEX idx_drivers_tenant ON drivers(tenant_id);

ALTER TABLE trips ADD COLUMN tenant_id BIGINT NULL;
UPDATE trips SET tenant_id = 1 WHERE tenant_id IS NULL;
ALTER TABLE trips MODIFY COLUMN tenant_id BIGINT NOT NULL DEFAULT 1;
ALTER TABLE trips ADD CONSTRAINT fk_trips_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE;
CREATE INDEX idx_trips_tenant ON trips(tenant_id);

ALTER TABLE trip_status_history ADD COLUMN tenant_id BIGINT NULL;
UPDATE trip_status_history SET tenant_id = 1 WHERE tenant_id IS NULL;
ALTER TABLE trip_status_history MODIFY COLUMN tenant_id BIGINT NOT NULL DEFAULT 1;
ALTER TABLE trip_status_history ADD CONSTRAINT fk_trip_hist_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE;
CREATE INDEX idx_trip_hist_tenant ON trip_status_history(tenant_id);

ALTER TABLE driver_performance_metrics ADD COLUMN tenant_id BIGINT NULL;
UPDATE driver_performance_metrics SET tenant_id = 1 WHERE tenant_id IS NULL;
ALTER TABLE driver_performance_metrics MODIFY COLUMN tenant_id BIGINT NOT NULL DEFAULT 1;
ALTER TABLE driver_performance_metrics ADD CONSTRAINT fk_driver_perf_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE;
CREATE INDEX idx_driver_perf_tenant ON driver_performance_metrics(tenant_id);

-- 7. Scope Module 4 (Shipments & Routes)
ALTER TABLE shipments ADD COLUMN tenant_id BIGINT NULL;
UPDATE shipments SET tenant_id = 1 WHERE tenant_id IS NULL;
ALTER TABLE shipments MODIFY COLUMN tenant_id BIGINT NOT NULL DEFAULT 1;
ALTER TABLE shipments ADD CONSTRAINT fk_shipments_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE;
CREATE INDEX idx_shipments_tenant ON shipments(tenant_id);

ALTER TABLE shipment_waypoints ADD COLUMN tenant_id BIGINT NULL;
UPDATE shipment_waypoints SET tenant_id = 1 WHERE tenant_id IS NULL;
ALTER TABLE shipment_waypoints MODIFY COLUMN tenant_id BIGINT NOT NULL DEFAULT 1;
ALTER TABLE shipment_waypoints ADD CONSTRAINT fk_waypoints_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE;
CREATE INDEX idx_waypoints_tenant ON shipment_waypoints(tenant_id);

ALTER TABLE shipment_timeline_events ADD COLUMN tenant_id BIGINT NULL;
UPDATE shipment_timeline_events SET tenant_id = 1 WHERE tenant_id IS NULL;
ALTER TABLE shipment_timeline_events MODIFY COLUMN tenant_id BIGINT NOT NULL DEFAULT 1;
ALTER TABLE shipment_timeline_events ADD CONSTRAINT fk_timeline_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE;
CREATE INDEX idx_timeline_tenant ON shipment_timeline_events(tenant_id);

-- 8. Scope Module 5 (Tracking & Control Tower)
ALTER TABLE telemetry_logs ADD COLUMN tenant_id BIGINT NULL;
UPDATE telemetry_logs SET tenant_id = 1 WHERE tenant_id IS NULL;
ALTER TABLE telemetry_logs MODIFY COLUMN tenant_id BIGINT NOT NULL DEFAULT 1;
ALTER TABLE telemetry_logs ADD CONSTRAINT fk_telemetry_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE;
CREATE INDEX idx_telemetry_tenant ON telemetry_logs(tenant_id);

ALTER TABLE geofences ADD COLUMN tenant_id BIGINT NULL;
UPDATE geofences SET tenant_id = 1 WHERE tenant_id IS NULL;
ALTER TABLE geofences MODIFY COLUMN tenant_id BIGINT NOT NULL DEFAULT 1;
ALTER TABLE geofences ADD CONSTRAINT fk_geofences_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE;
CREATE INDEX idx_geofences_tenant ON geofences(tenant_id);

ALTER TABLE alerts ADD COLUMN tenant_id BIGINT NULL;
UPDATE alerts SET tenant_id = 1 WHERE tenant_id IS NULL;
ALTER TABLE alerts MODIFY COLUMN tenant_id BIGINT NOT NULL DEFAULT 1;
ALTER TABLE alerts ADD CONSTRAINT fk_alerts_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE;
CREATE INDEX idx_alerts_tenant ON alerts(tenant_id);

ALTER TABLE control_tower_kpis ADD COLUMN tenant_id BIGINT NULL;
UPDATE control_tower_kpis SET tenant_id = 1 WHERE tenant_id IS NULL;
ALTER TABLE control_tower_kpis MODIFY COLUMN tenant_id BIGINT NOT NULL DEFAULT 1;
ALTER TABLE control_tower_kpis ADD CONSTRAINT fk_kpis_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE;
CREATE INDEX idx_kpis_tenant ON control_tower_kpis(tenant_id);

ALTER TABLE geofence_vehicle_states ADD COLUMN tenant_id BIGINT NULL;
UPDATE geofence_vehicle_states SET tenant_id = 1 WHERE tenant_id IS NULL;
ALTER TABLE geofence_vehicle_states MODIFY COLUMN tenant_id BIGINT NOT NULL DEFAULT 1;
ALTER TABLE geofence_vehicle_states ADD CONSTRAINT fk_gvs_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE;
CREATE INDEX idx_gvs_tenant ON geofence_vehicle_states(tenant_id);
