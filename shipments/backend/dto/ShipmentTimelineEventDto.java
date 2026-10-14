package com.fleetx.shipments.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;

public record ShipmentTimelineEventDto(
        Long id,
        Long shipmentId,
        String eventType,
        String locationSummary,
        String remarks,
        Instant createdAt
) {
    @JsonProperty("status")
    public String status() {
        return eventType;
    }

    @JsonProperty("locationName")
    public String locationName() {
        return locationSummary;
    }

    @JsonProperty("description")
    public String description() {
        return remarks;
    }

    @JsonProperty("recordedAt")
    public Instant recordedAt() {
        return createdAt;
    }
}
