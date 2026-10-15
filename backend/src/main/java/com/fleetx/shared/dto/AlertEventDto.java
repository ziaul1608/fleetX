package com.fleetx.shared.dto;

import java.time.Instant;

public record AlertEventDto(
        String alertType,
        String severity,
        String entityType,
        Long entityId,
        String description,
        Instant timestamp
) {
}
