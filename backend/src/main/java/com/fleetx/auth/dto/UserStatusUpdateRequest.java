package com.fleetx.auth.dto;

import jakarta.validation.constraints.NotNull;

public record UserStatusUpdateRequest(
        @NotNull(message = "Active status is required.")
        Boolean active,

        String reason
) {
}
