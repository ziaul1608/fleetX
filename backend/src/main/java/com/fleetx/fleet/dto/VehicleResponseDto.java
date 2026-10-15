package com.fleetx.fleet.dto;

import java.time.Instant;

public record VehicleResponseDto(
        Long id,
        String vin,
        String licensePlate,
        String make,
        String model,
        Integer modelYear,
        String vehicleType,
        String fuelType,
        Double capacityKg,
        Double capacityVolumeM3,
        Double odometerKm,
        String status,
        Boolean active,
        Double averageFuelEfficiency,
        Boolean serviceDue,
        Instant createdAt
) {
}
