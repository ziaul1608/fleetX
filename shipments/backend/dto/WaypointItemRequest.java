package com.fleetx.shipments.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record WaypointItemRequest(
        @JsonAlias({"locationName", "waypointName"})
        @NotBlank(message = "Waypoint name is required")
        String waypointName,

        @NotNull(message = "Latitude is required")
        Double latitude,

        @NotNull(message = "Longitude is required")
        Double longitude,

        @NotNull(message = "Sequence order is required")
        Integer sequenceOrder,

        String stopType
) {
}
