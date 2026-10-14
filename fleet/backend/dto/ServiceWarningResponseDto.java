package com.fleetx.fleet.dto;

import java.time.Instant;

public record ServiceWarningResponseDto(
        Long id,
        Long vehicleId,
        String licensePlate,
        String warningType,
        String severity,
        String message,
        Boolean resolved,
        Instant createdAt
) {
}
