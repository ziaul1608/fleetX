package com.fleetx.drivers.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;

public record TripCreateRequest(
        @NotNull(message = "Vehicle ID is required")
        Long vehicleId,

        @NotNull(message = "Driver ID is required")
        Long driverId,

        @NotBlank(message = "Origin address is required")
        String originAddress,

        @NotBlank(message = "Destination address is required")
        String destinationAddress,

        @NotNull(message = "Scheduled start time is required")
        Instant scheduledStartTime,

        Instant scheduledEndTime,

        BigDecimal distanceKm,

        String notes
) {
}
