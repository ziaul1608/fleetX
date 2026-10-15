-- ==============================================================================
-- Module 3: Driver & Trip Operations Schema Enhancements
-- Range: V300 - V399
-- ==============================================================================

ALTER TABLE drivers ADD COLUMN full_name VARCHAR(120) NOT NULL DEFAULT 'Commercial Driver';
ALTER TABLE drivers ADD COLUMN phone_number VARCHAR(30) NOT NULL DEFAULT '+91 90000 00000';
ALTER TABLE drivers ADD COLUMN email VARCHAR(180) NULL;
ALTER TABLE drivers ADD COLUMN experience_years INT NOT NULL DEFAULT 0;
ALTER TABLE drivers ADD COLUMN active BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE drivers ADD COLUMN operational_notes TEXT NULL;
ALTER TABLE drivers ADD COLUMN created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6);
ALTER TABLE drivers ADD COLUMN updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6);

ALTER TABLE drivers MODIFY COLUMN user_id BIGINT NULL;

CREATE INDEX idx_drivers_active ON drivers(active);

ALTER TABLE trips ADD COLUMN scheduled_end_time DATETIME(6) NULL;
ALTER TABLE trips ADD COLUMN distance_km DECIMAL(10,2) NULL;
ALTER TABLE trips ADD COLUMN notes TEXT NULL;
ALTER TABLE trips ADD COLUMN updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6);

ALTER TABLE driver_performance_metrics ADD COLUMN total_distance_km DECIMAL(12,2) NOT NULL DEFAULT 0.00;
ALTER TABLE driver_performance_metrics ADD COLUMN total_trips_cancelled INT NOT NULL DEFAULT 0;
ALTER TABLE driver_performance_metrics ADD COLUMN last_calculated_at DATETIME(6) NULL;
