package com.fleetx.auth.dto;

import java.time.Instant;

public record AuditLogResponseDto(
        Long id,
        Long userId,
        String action,
        String entityType,
        Long entityId,
        String ipAddress,
        String detailsJson,
        Instant createdAt
) {
}
