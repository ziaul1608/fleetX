package com.fleetx.drivers.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record DriverPerformanceResponseDto(
        Long driverId,
        String driverName,
        Integer totalTripsCompleted,
        Integer onTimeDeliveries,
        BigDecimal onTimeDeliveryRate,
        Integer safetyViolationCount,
        Integer totalTripsCancelled,
        BigDecimal totalDistanceKm,
        BigDecimal ratingScore,
        Instant lastCalculatedAt
) {
}
