package com.fleetx.drivers.dto;

import java.time.Instant;

public record TripStatusHistoryResponseDto(
        Long id,
        Long tripId,
        String fromStatus,
        String toStatus,
        String remarks,
        Long updatedBy,
        Instant createdAt
) {
}
