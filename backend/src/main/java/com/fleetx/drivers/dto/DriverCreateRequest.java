package com.fleetx.drivers.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record DriverCreateRequest(
        Long userId,

        @NotBlank(message = "Driver full name is required")
        String fullName,

        @NotBlank(message = "Contact phone number is required")
        String phoneNumber,

        String email,

        @NotBlank(message = "License number is required")
        String licenseNumber,

        String licenseCategory,

        @NotNull(message = "License expiry date is required")
        LocalDate licenseExpiryDate,

        LocalDate medicalCertExpiryDate,

        Integer experienceYears,

        String availabilityStatus,

        String operationalNotes
) {
}
