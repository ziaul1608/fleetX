package com.fleetx.drivers.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.Instant;

public record TripResponseDto(
        Long id,
        String tripCode,
        Long vehicleId,
        Long driverId,
        String originAddress,
        String destinationAddress,
        String status,
        Instant scheduledStartTime,
        Instant scheduledEndTime,
        Instant actualStartTime,
        Instant actualEndTime,
        BigDecimal distanceKm,
        String notes,
        Instant createdAt,
        Instant updatedAt,
        String vehicleLicensePlate,
        String vehicleMakeModel,
        String driverName,
        String driverPhone
) {
    @JsonProperty("tripNumber")
    public String tripNumber() {
        return tripCode;
    }
}
