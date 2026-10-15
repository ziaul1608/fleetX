package com.fleetx.tracking.dto;

import com.fleetx.shared.dto.ShipmentSummaryDto;

import java.time.Instant;
import java.util.List;

public record SimulatorSessionDto(
        Long vehicleId,
        String licensePlate,
        String vehicleMakeModel,
        String vehicleStatus,
        Long tripId,
        String tripCode,
        String tripStatus,
        String simulatorStatus,
        Double latitude,
        Double longitude,
        Double speedKmh,
        Double heading,
        Double currentOdometerKm,
        int currentWaypointIndex,
        int totalWaypoints,
        double segmentProgress,
        int tickIntervalSeconds,
        double speedMultiplier,
        boolean simulated,
        String dataSource,
        Instant lastUpdatedAt,
        List<SimulatorWaypointDto> waypoints,
        List<ShipmentSummaryDto> assignedShipments
) {
}
