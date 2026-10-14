package com.fleetx.shipments.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record WaypointsUpdateRequest(
        @NotEmpty(message = "At least two route waypoints (Origin and Destination) are required")
        @Valid
        List<WaypointItemRequest> waypoints
) {
}
