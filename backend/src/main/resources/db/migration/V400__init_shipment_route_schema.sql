-- ==============================================================================
-- Module 4: Shipment & Routes Schema Baseline
-- Reserved Range: V400 - V499
-- ==============================================================================

CREATE TABLE IF NOT EXISTS shipments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tracking_number VARCHAR(30) NOT NULL UNIQUE,
    trip_id BIGINT NULL,
    sender_name VARCHAR(120) NOT NULL,
    receiver_name VARCHAR(120) NOT NULL,
    weight_kg DECIMAL(10,2) NOT NULL,
    volume_m3 DECIMAL(8,2) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'ORDERED',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_shipment_trip FOREIGN KEY (trip_id) REFERENCES trips(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE INDEX idx_shipments_tracking ON shipments(tracking_number);
CREATE INDEX idx_shipments_trip ON shipments(trip_id);

CREATE TABLE IF NOT EXISTS shipment_waypoints (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    shipment_id BIGINT NOT NULL,
    waypoint_name VARCHAR(120) NOT NULL,
    latitude DECIMAL(10,7) NOT NULL,
    longitude DECIMAL(10,7) NOT NULL,
    sequence_order INT NOT NULL,
    stop_type VARCHAR(30) NOT NULL,
    estimated_arrival DATETIME(6) NULL,
    CONSTRAINT fk_waypoint_shipment FOREIGN KEY (shipment_id) REFERENCES shipments(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE INDEX idx_waypoint_seq ON shipment_waypoints(shipment_id, sequence_order);

CREATE TABLE IF NOT EXISTS shipment_timeline_events (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    shipment_id BIGINT NOT NULL,
    event_type VARCHAR(50) NOT NULL,
    location_summary VARCHAR(255) NULL,
    remarks TEXT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_timeline_shipment FOREIGN KEY (shipment_id) REFERENCES shipments(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE INDEX idx_timeline_shipment ON shipment_timeline_events(shipment_id, created_at);
