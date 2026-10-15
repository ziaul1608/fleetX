package com.fleetx.shipments.dto;

import jakarta.validation.constraints.NotNull;

public record ShipmentAssignTripRequest(
        @NotNull(message = "Trip ID is required")
        Long tripId
) {
}
