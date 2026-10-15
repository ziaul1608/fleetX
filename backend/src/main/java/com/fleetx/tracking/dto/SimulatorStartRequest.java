package com.fleetx.tracking.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import java.util.List;

public record SimulatorStartRequest(
        Long vehicleId,
        Long tripId,

        @Min(value = 1, message = "Tick interval must be at least 1 second")
        @Max(value = 60, message = "Tick interval cannot exceed 60 seconds")
        Integer tickIntervalSeconds,

        @DecimalMin(value = "0.5", message = "Speed multiplier must be at least 0.5x")
        @DecimalMax(value = "20.0", message = "Speed multiplier cannot exceed 20.0x")
        Double speedMultiplier,

        @DecimalMin(value = "0.0", message = "Base speed cannot be negative")
        @DecimalMax(value = "250.0", message = "Base speed cannot exceed 250 km/h")
        Double baseSpeedKmh,

        @Valid
        List<SimulatorWaypointDto> waypoints
) {
}
