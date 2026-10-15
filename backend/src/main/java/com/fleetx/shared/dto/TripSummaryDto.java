package com.fleetx.shared.dto;

public record TripSummaryDto(
        Long id,
        String tripCode,
        Long vehicleId,
        Long driverId,
        String status,
        String originAddress,
        String destinationAddress
) {
}
