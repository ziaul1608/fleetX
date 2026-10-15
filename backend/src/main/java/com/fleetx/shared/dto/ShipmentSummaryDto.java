package com.fleetx.shared.dto;

public record ShipmentSummaryDto(
        Long id,
        String trackingNumber,
        Long tripId,
        Double weightKg,
        String status
) {
}
