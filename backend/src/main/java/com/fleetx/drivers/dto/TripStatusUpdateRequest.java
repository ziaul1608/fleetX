package com.fleetx.drivers.dto;

import jakarta.validation.constraints.NotBlank;

public record TripStatusUpdateRequest(
        @NotBlank(message = "Target status is required")
        String targetStatus,

        String remarks
) {
}
