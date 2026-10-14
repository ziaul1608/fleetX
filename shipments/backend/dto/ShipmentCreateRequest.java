package com.fleetx.shipments.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.Instant;

public record ShipmentCreateRequest(
        String trackingNumber,

        @NotBlank(message = "Sender name is required")
        String senderName,

        @JsonAlias({"originCity", "senderAddress"})
        @NotBlank(message = "Sender pickup address is required")
        String senderAddress,

        @NotBlank(message = "Receiver name is required")
        String receiverName,

        @JsonAlias({"destinationCity", "receiverAddress"})
        @NotBlank(message = "Receiver destination address is required")
        String receiverAddress,

        String cargoDescription,

        @NotNull(message = "Cargo weight (kg) is required")
        @Positive(message = "Cargo weight must be greater than zero")
        Double weightKg,

        @NotNull(message = "Cargo volume (m³) is required")
        @Positive(message = "Cargo volume must be greater than zero")
        Double volumeM3,

        Instant promisedDeliveryTime,

        Long tripId
) {
    public ShipmentCreateRequest {
        if (volumeM3 == null) {
            volumeM3 = 1.0;
        }
    }
}
