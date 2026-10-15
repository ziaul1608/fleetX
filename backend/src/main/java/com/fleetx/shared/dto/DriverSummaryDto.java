package com.fleetx.shared.dto;

import java.time.LocalDate;

public record DriverSummaryDto(
        Long id,
        Long userId,
        String licenseNumber,
        String licenseCategory,
        String availabilityStatus,
        LocalDate licenseExpiryDate
) {
}
