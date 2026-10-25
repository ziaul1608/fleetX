package com.fleetx.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.Set;

public record UserCreateRequest(
        @NotBlank(message = "Email address is required.")
        @Email(message = "Email must be a valid email format.")
        String email,

        @NotBlank(message = "Full name is required.")
        @Size(min = 2, max = 120, message = "Full name must be between 2 and 120 characters.")
        String fullName,

        @Size(max = 64, message = "Password must be at most 64 characters if provided.")
        String password,

        @NotEmpty(message = "At least one role must be assigned.")
        Set<String> roles
) {
}
