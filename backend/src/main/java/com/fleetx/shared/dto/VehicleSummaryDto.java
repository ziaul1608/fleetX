package com.fleetx.shared.dto;

public record VehicleSummaryDto(
        Long id,
        String vin,
        String licensePlate,
        String make,
        String model,
        String status,
        Double odometerKm
) {
}
