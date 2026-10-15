package com.fleetx.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdminPasswordResetRequest(
        @NotBlank(message = "New password is required.")
        @Size(min = 8, max = 64, message = "Password must be at least 8 characters.")
        String newPassword
) {
}
