package com.fleetx.shipments.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;

public record ShipmentStatusUpdateRequest(
        @JsonAlias({"status", "targetStatus"})
        @NotBlank(message = "Target status is required")
        String targetStatus,

        @JsonAlias({"locationName", "locationSummary"})
        String locationSummary,

        @JsonAlias({"description", "remarks"})
        String remarks
) {
}
