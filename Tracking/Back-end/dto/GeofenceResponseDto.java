package com.fleetx.tracking.dto;

import java.time.Instant;
import java.util.List;

public record GeofenceResponseDto(
        Long id,
        String name,
        Double centerLatitude,
        Double centerLongitude,
        Double radiusMeters,
        String fenceType,
        String shapeType,
        List<List<Double>> polygonCoordinates,
        Double speedLimitKmh,
        Boolean active,
        Instant createdAt
) {
}
