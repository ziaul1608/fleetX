package com.fleetx.auth.dto;

import java.time.Instant;
import java.util.List;

public record UserResponseDto(
        Long userId,
        String email,
        String fullName,
        List<String> roles,
        boolean active,
        Instant createdAt,
        Long tenantId,
        String companyName,
        String warehouseName
) {
    public UserResponseDto(Long userId, String email, String fullName, List<String> roles) {
        this(userId, email, fullName, roles, true, Instant.now(), null, null, null);
    }

    public UserResponseDto(Long userId, String email, String fullName, List<String> roles, boolean active, Instant createdAt) {
        this(userId, email, fullName, roles, active, createdAt, null, null, null);
    }
}
