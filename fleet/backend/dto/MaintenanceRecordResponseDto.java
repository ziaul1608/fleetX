package com.fleetx.fleet.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record MaintenanceRecordResponseDto(
        Long id,
        Long vehicleId,
        String licensePlate,
        String serviceType,
        String description,
        BigDecimal cost,
        LocalDate serviceDate,
        Double odometerAtService,
        Double nextServiceDueKm,
        LocalDate nextServiceDueDate,
        String status,
        Boolean isOverdue,
        Instant createdAt
) {
}
