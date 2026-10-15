package com.fleetx.drivers.dto;

import jakarta.validation.constraints.NotBlank;

public record DriverStatusUpdateRequest(
        @NotBlank(message = "Availability status is required")
        String availabilityStatus
) {
}
