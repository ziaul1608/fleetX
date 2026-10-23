package com.fleetx.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PasswordChangeRequest(
        @NotBlank(message = "Current password is required.")
        String currentPassword,

        @NotBlank(message = "New password is required.")
        @Size(min = 8, max = 64, message = "New password must be between 8 and 64 characters.")
        String newPassword,

        @NotBlank(message = "Confirm new password is required.")
        String confirmPassword
) {
}
