package com.fleetx.fleet.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record FuelRecordResponseDto(
        Long id,
        Long vehicleId,
        String licensePlate,
        Double liters,
        BigDecimal pricePerLiter,
        BigDecimal totalCost,
        Double odometerAtFueling,
        String fuelStation,
        Instant fuelDate,
        Double efficiencyKmPerLiter
) {
}
