-- ==============================================================================
-- Module 6 Hardening: Multi-Tenant Schema Security & Foreign Key Protection
-- Version: V601
-- ==============================================================================

-- Prevent catastrophic cascade deletion of tenant business data by converting
-- all tenant foreign keys from ON DELETE CASCADE to ON DELETE RESTRICT.
-- Furthermore, drop DEFAULT 1 from tenant_id columns to mandate explicit tenant scoping.

-- 1. Identity & Audit
ALTER TABLE users DROP FOREIGN KEY fk_users_tenant;
ALTER TABLE users ALTER COLUMN tenant_id DROP DEFAULT;
ALTER TABLE users ADD CONSTRAINT fk_users_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE RESTRICT;

ALTER TABLE audit_logs DROP FOREIGN KEY fk_audit_tenant;
ALTER TABLE audit_logs ALTER COLUMN tenant_id DROP DEFAULT;
ALTER TABLE audit_logs ADD CONSTRAINT fk_audit_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE RESTRICT;

-- 2. Fleet & Maintenance
ALTER TABLE vehicles DROP FOREIGN KEY fk_vehicles_tenant;
ALTER TABLE vehicles ALTER COLUMN tenant_id DROP DEFAULT;
ALTER TABLE vehicles ADD CONSTRAINT fk_vehicles_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE RESTRICT;

ALTER TABLE fuel_records DROP FOREIGN KEY fk_fuel_tenant;
ALTER TABLE fuel_records ALTER COLUMN tenant_id DROP DEFAULT;
ALTER TABLE fuel_records ADD CONSTRAINT fk_fuel_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE RESTRICT;

ALTER TABLE maintenance_records DROP FOREIGN KEY fk_maint_tenant;
ALTER TABLE maintenance_records ALTER COLUMN tenant_id DROP DEFAULT;
ALTER TABLE maintenance_records ADD CONSTRAINT fk_maint_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE RESTRICT;

ALTER TABLE service_warnings DROP FOREIGN KEY fk_warnings_tenant;
ALTER TABLE service_warnings ALTER COLUMN tenant_id DROP DEFAULT;
ALTER TABLE service_warnings ADD CONSTRAINT fk_warnings_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE RESTRICT;

-- 3. Drivers & Trips
ALTER TABLE drivers DROP FOREIGN KEY fk_drivers_tenant;
ALTER TABLE drivers ALTER COLUMN tenant_id DROP DEFAULT;
ALTER TABLE drivers ADD CONSTRAINT fk_drivers_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE RESTRICT;

ALTER TABLE trips DROP FOREIGN KEY fk_trips_tenant;
ALTER TABLE trips ALTER COLUMN tenant_id DROP DEFAULT;
ALTER TABLE trips ADD CONSTRAINT fk_trips_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE RESTRICT;

ALTER TABLE trip_status_history DROP FOREIGN KEY fk_trip_hist_tenant;
ALTER TABLE trip_status_history ALTER COLUMN tenant_id DROP DEFAULT;
ALTER TABLE trip_status_history ADD CONSTRAINT fk_trip_hist_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE RESTRICT;

ALTER TABLE driver_performance_metrics DROP FOREIGN KEY fk_driver_perf_tenant;
ALTER TABLE driver_performance_metrics ALTER COLUMN tenant_id DROP DEFAULT;
ALTER TABLE driver_performance_metrics ADD CONSTRAINT fk_driver_perf_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE RESTRICT;

-- 4. Shipments & Routes
ALTER TABLE shipments DROP FOREIGN KEY fk_shipments_tenant;
ALTER TABLE shipments ALTER COLUMN tenant_id DROP DEFAULT;
ALTER TABLE shipments ADD CONSTRAINT fk_shipments_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE RESTRICT;

ALTER TABLE shipment_waypoints DROP FOREIGN KEY fk_waypoints_tenant;
ALTER TABLE shipment_waypoints ALTER COLUMN tenant_id DROP DEFAULT;
ALTER TABLE shipment_waypoints ADD CONSTRAINT fk_waypoints_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE RESTRICT;

ALTER TABLE shipment_timeline_events DROP FOREIGN KEY fk_timeline_tenant;
ALTER TABLE shipment_timeline_events ALTER COLUMN tenant_id DROP DEFAULT;
ALTER TABLE shipment_timeline_events ADD CONSTRAINT fk_timeline_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE RESTRICT;

-- 5. Tracking & Control Tower
ALTER TABLE telemetry_logs DROP FOREIGN KEY fk_telemetry_tenant;
ALTER TABLE telemetry_logs ALTER COLUMN tenant_id DROP DEFAULT;
ALTER TABLE telemetry_logs ADD CONSTRAINT fk_telemetry_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE RESTRICT;

ALTER TABLE geofences DROP FOREIGN KEY fk_geofences_tenant;
ALTER TABLE geofences ALTER COLUMN tenant_id DROP DEFAULT;
ALTER TABLE geofences ADD CONSTRAINT fk_geofences_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE RESTRICT;

ALTER TABLE alerts DROP FOREIGN KEY fk_alerts_tenant;
ALTER TABLE alerts ALTER COLUMN tenant_id DROP DEFAULT;
ALTER TABLE alerts ADD CONSTRAINT fk_alerts_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE RESTRICT;

ALTER TABLE control_tower_kpis DROP FOREIGN KEY fk_kpis_tenant;
ALTER TABLE control_tower_kpis ALTER COLUMN tenant_id DROP DEFAULT;
ALTER TABLE control_tower_kpis ADD CONSTRAINT fk_kpis_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE RESTRICT;

ALTER TABLE geofence_vehicle_states DROP FOREIGN KEY fk_gvs_tenant;
ALTER TABLE geofence_vehicle_states ALTER COLUMN tenant_id DROP DEFAULT;
ALTER TABLE geofence_vehicle_states ADD CONSTRAINT fk_gvs_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE RESTRICT;
