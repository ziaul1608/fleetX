package com.fleetx.drivers.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record DriverUpdateRequest(
        @NotBlank(message = "Driver full name is required")
        String fullName,

        @NotBlank(message = "Contact phone number is required")
        String phoneNumber,

        String email,

        String licenseCategory,

        @NotNull(message = "License expiry date is required")
        LocalDate licenseExpiryDate,

        LocalDate medicalCertExpiryDate,

        Integer experienceYears,

        String availabilityStatus,

        Boolean active,

        String operationalNotes
) {
}
