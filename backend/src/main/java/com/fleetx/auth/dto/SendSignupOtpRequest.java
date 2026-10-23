package com.fleetx.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SendSignupOtpRequest(
        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        String email,

        @NotBlank(message = "Company / Organization name is required")
        @Size(min = 2, max = 150, message = "Company name must be between 2 and 150 characters")
        String companyName,

        @NotBlank(message = "Warehouse facility name is required")
        @Size(min = 2, max = 150, message = "Warehouse name must be between 2 and 150 characters")
        String warehouseName,

        @NotBlank(message = "Full name is required")
        @Size(min = 2, max = 100, message = "Full name must be between 2 and 100 characters")
        String fullName
) {}
