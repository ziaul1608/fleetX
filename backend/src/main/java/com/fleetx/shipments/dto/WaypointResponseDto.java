package com.fleetx.shipments.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.Instant;

public record WaypointResponseDto(
        Long id,
        Long shipmentId,
        String waypointName,
        BigDecimal latitude,
        BigDecimal longitude,
        Integer sequenceOrder,
        String stopType,
        Instant estimatedArrival
) {
    @JsonProperty("locationName")
    public String locationName() {
        return waypointName;
    }

    @JsonProperty("estimatedArrivalTime")
    public Instant estimatedArrivalTime() {
        return estimatedArrival;
    }

    @JsonProperty("legDistanceKm")
    public Double legDistanceKm() {
        return 0.0;
    }

    @JsonProperty("cumulativeDistanceKm")
    public Double cumulativeDistanceKm() {
        return 0.0;
    }
}
