package com.fleetx.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record VerifySignupOtpRequest(
        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        String email,

        @NotBlank(message = "Verification code is required")
        @Pattern(regexp = "^[0-9]{6}$", message = "Verification code must be exactly 6 digits")
        String otp,

        @NotBlank(message = "Company name is required")
        @Size(min = 2, max = 150, message = "Company name must be between 2 and 150 characters")
        String companyName,

        @NotBlank(message = "Warehouse name is required")
        @Size(min = 2, max = 150, message = "Warehouse name must be between 2 and 150 characters")
        String warehouseName,

        @NotBlank(message = "Full name is required")
        @Size(min = 2, max = 100, message = "Full name must be between 2 and 100 characters")
        String fullName
) {}
