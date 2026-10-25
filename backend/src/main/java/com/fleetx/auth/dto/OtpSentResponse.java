package com.fleetx.auth.dto;

public record OtpSentResponse(
        String email,
        String purpose,
        int expiresInSeconds,
        int cooldownSeconds,
        String message
) {}
