package com.fleetx.tracking.dto;

import java.time.Instant;

public record AlertResponseDto(
        Long id,
        String alertType,
        String severity,
        String entityType,
        Long entityId,
        Long geofenceId,
        String message,
        Boolean acknowledged,
        String acknowledgedBy,
        Instant acknowledgedAt,
        Instant createdAt
) {
    public Long getAlertId() {
        return id;
    }

    public String getDescription() {
        return message;
    }

    public Instant getTimestamp() {
        return createdAt;
    }
}
