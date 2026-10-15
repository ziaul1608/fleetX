package com.fleetx.shared.contract;

import java.time.Instant;

public interface AlertPublisher {
    /**
     * Publishes a real-time operational alert across the system and STOMP topic.
     * @param alertType Classification code (e.g., "GEOFENCE_BREACH", "OVERSPEED", "SERVICE_DUE").
     * @param severity Level: "INFO", "WARNING", "CRITICAL".
     * @param entityType Associated entity type: "VEHICLE", "TRIP", "DRIVER".
     * @param entityId Primary key of the associated entity.
     * @param description Human-readable description of the incident.
     * @param timestamp Occurrence timestamp in UTC.
     */
    void publishAlert(String alertType, String severity, String entityType, Long entityId, String description, Instant timestamp);
}
