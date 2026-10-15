-- ==============================================================================
-- Module 2: Fleet & Assets Schema Enhancements
-- Range: V200 - V299
-- ==============================================================================

ALTER TABLE vehicles ADD COLUMN active BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE vehicles ADD COLUMN updated_at DATETIME(6) NULL DEFAULT CURRENT_TIMESTAMP(6);

CREATE INDEX idx_vehicles_active ON vehicles(active);

ALTER TABLE fuel_records ADD COLUMN price_per_liter DECIMAL(10,2) NULL;
ALTER TABLE fuel_records ADD COLUMN efficiency_km_per_liter DECIMAL(8,2) NULL;

ALTER TABLE maintenance_records ADD COLUMN status VARCHAR(30) NOT NULL DEFAULT 'COMPLETED';
ALTER TABLE maintenance_records ADD COLUMN created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6);

CREATE INDEX idx_maint_status ON maintenance_records(status);
