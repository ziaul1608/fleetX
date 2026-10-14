package com.fleetx.drivers.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record DriverResponseDto(
        Long id,
        Long userId,
        String fullName,
        String phoneNumber,
        String email,
        String licenseNumber,
        String licenseCategory,
        LocalDate licenseExpiryDate,
        LocalDate medicalCertExpiryDate,
        Integer experienceYears,
        String availabilityStatus,
        Boolean active,
        String operationalNotes,
        Instant createdAt,
        Instant updatedAt,
        BigDecimal safetyScore,
        Integer completedTripsCount
) {
}
