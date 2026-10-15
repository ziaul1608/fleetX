package com.fleetx.tracking.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.List;

public record GeofenceCreateRequest(
        @NotBlank(message = "Geofence name is required")
        String name,

        @NotNull(message = "Center latitude is required")
        @DecimalMin(value = "-90.0", message = "Center latitude must be >= -90")
        @DecimalMax(value = "90.0", message = "Center latitude must be <= 90")
        Double centerLatitude,

        @NotNull(message = "Center longitude is required")
        @DecimalMin(value = "-180.0", message = "Center longitude must be >= -180")
        @DecimalMax(value = "180.0", message = "Center longitude must be <= 180")
        Double centerLongitude,

        @NotNull(message = "Radius in meters is required")
        @Positive(message = "Radius must be strictly positive")
        Double radiusMeters,

        @NotBlank(message = "Fence type is required (DEPOT, CUSTOMER_HUB, RESTRICTED)")
        String fenceType,

        String shapeType,

        List<List<Double>> polygonCoordinates,

        @Positive(message = "Speed limit must be positive when specified")
        Double speedLimitKmh
) {
}
