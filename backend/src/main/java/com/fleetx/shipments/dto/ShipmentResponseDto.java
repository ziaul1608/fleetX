package com.fleetx.shipments.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.Instant;

public record ShipmentResponseDto(
        Long id,
        String trackingNumber,
        Long tripId,
        String tripCode,
        String senderName,
        String senderAddress,
        String receiverName,
        String receiverAddress,
        String cargoDescription,
        Double weightKg,
        Double volumeM3,
        String status,
        Instant promisedDeliveryTime,
        BigDecimal estimatedDistanceKm,
        Integer estimatedDurationMinutes,
        Instant estimatedArrivalTime,
        Instant deliveredAt,
        String deliveryNotes,
        Boolean isLate,
        Instant createdAt,
        Instant updatedAt
) {
    @JsonProperty("tripNumber")
    public String tripNumber() {
        return tripCode;
    }

    @JsonProperty("originCity")
    public String originCity() {
        return senderAddress;
    }

    @JsonProperty("destinationCity")
    public String destinationCity() {
        return receiverAddress;
    }

    @JsonProperty("delayed")
    public Boolean delayed() {
        return Boolean.TRUE.equals(isLate) || "DELAYED".equalsIgnoreCase(status);
    }
}
