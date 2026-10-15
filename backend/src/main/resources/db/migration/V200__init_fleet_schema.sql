-- ==============================================================================
-- Module 2: Fleet & Assets Schema Baseline
-- Reserved Range: V200 - V299
-- ==============================================================================

CREATE TABLE IF NOT EXISTS vehicles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    vin VARCHAR(17) NOT NULL UNIQUE,
    license_plate VARCHAR(20) NOT NULL UNIQUE,
    make VARCHAR(60) NOT NULL,
    model VARCHAR(60) NOT NULL,
    model_year INT NOT NULL,
    vehicle_type VARCHAR(30) NOT NULL,
    fuel_type VARCHAR(20) NOT NULL,
    capacity_kg DECIMAL(10,2) NOT NULL,
    capacity_volume_m3 DECIMAL(8,2) NOT NULL,
    odometer_km DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    status VARCHAR(30) NOT NULL DEFAULT 'AVAILABLE',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE INDEX idx_vehicles_status ON vehicles(status);
CREATE INDEX idx_vehicles_license_plate ON vehicles(license_plate);

CREATE TABLE IF NOT EXISTS fuel_records (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    vehicle_id BIGINT NOT NULL,
    liters DECIMAL(8,2) NOT NULL,
    total_cost DECIMAL(10,2) NOT NULL,
    odometer_at_fueling DECIMAL(12,2) NOT NULL,
    fuel_station VARCHAR(120) NULL,
    fuel_date DATETIME(6) NOT NULL,
    CONSTRAINT fk_fuel_vehicle FOREIGN KEY (vehicle_id) REFERENCES vehicles(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE INDEX idx_fuel_vehicle_date ON fuel_records(vehicle_id, fuel_date);

CREATE TABLE IF NOT EXISTS maintenance_records (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    vehicle_id BIGINT NOT NULL,
    service_type VARCHAR(40) NOT NULL,
    description TEXT NOT NULL,
    cost DECIMAL(10,2) NOT NULL,
    service_date DATE NOT NULL,
    odometer_at_service DECIMAL(12,2) NOT NULL,
    next_service_due_km DECIMAL(12,2) NULL,
    next_service_due_date DATE NULL,
    CONSTRAINT fk_maintenance_vehicle FOREIGN KEY (vehicle_id) REFERENCES vehicles(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE INDEX idx_maint_vehicle_date ON maintenance_records(vehicle_id, service_date);

CREATE TABLE IF NOT EXISTS service_warnings (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    vehicle_id BIGINT NOT NULL,
    warning_type VARCHAR(50) NOT NULL,
    severity VARCHAR(20) NOT NULL DEFAULT 'WARNING',
    message VARCHAR(255) NOT NULL,
    resolved BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_warning_vehicle FOREIGN KEY (vehicle_id) REFERENCES vehicles(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE INDEX idx_warning_vehicle_unresolved ON service_warnings(vehicle_id, resolved);
