package com.fleetx.tracking.dto;

import java.time.Instant;

public record TelemetryPingDto(
        Long vehicleId,
        Long tripId,
        Double latitude,
        Double longitude,
        Double speedKmh,
        Double heading,
        Double currentOdometerKm,
        Instant timestamp,
        boolean simulated,
        String waypointLabel,
        String dataSource
) {
    public TelemetryPingDto(
            Long vehicleId,
            Long tripId,
            Double latitude,
            Double longitude,
            Double speedKmh,
            Double heading,
            Double currentOdometerKm,
            Instant timestamp
    ) {
        this(
                vehicleId,
                tripId,
                latitude,
                longitude,
                speedKmh,
                heading,
                currentOdometerKm,
                timestamp,
                true,
                null,
                "SIMULATED_GPS_TELEMETRY"
        );
    }

    public Double getOdometerKm() {
        return currentOdometerKm;
    }
}
