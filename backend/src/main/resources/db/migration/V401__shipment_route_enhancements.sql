-- ==============================================================================
-- Module 4: Shipment & Route Management Schema Enhancements
-- Range: V400 - V499
-- ==============================================================================

ALTER TABLE shipments ADD COLUMN sender_address VARCHAR(255) NOT NULL DEFAULT 'Origin Dispatch Hub';
ALTER TABLE shipments ADD COLUMN receiver_address VARCHAR(255) NOT NULL DEFAULT 'Destination Delivery Hub';
ALTER TABLE shipments ADD COLUMN cargo_description TEXT NULL;
ALTER TABLE shipments ADD COLUMN promised_delivery_time DATETIME(6) NULL;
ALTER TABLE shipments ADD COLUMN estimated_distance_km DECIMAL(10,2) NULL;
ALTER TABLE shipments ADD COLUMN estimated_duration_minutes INT NULL;
ALTER TABLE shipments ADD COLUMN estimated_arrival_time DATETIME(6) NULL;
ALTER TABLE shipments ADD COLUMN delivered_at DATETIME(6) NULL;
ALTER TABLE shipments ADD COLUMN delivery_notes TEXT NULL;
ALTER TABLE shipments ADD COLUMN updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6);

CREATE INDEX idx_shipments_status ON shipments(status);
CREATE INDEX idx_shipments_promised ON shipments(promised_delivery_time);
