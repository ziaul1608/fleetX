package com.fleetx.shared.dto;

import java.time.Instant;
import java.util.List;

public record ApiErrorResponse(
        boolean success,
        int status,
        String errorCode,
        String message,
        String path,
        Instant timestamp,
        List<ValidationErrorDto> validationErrors
) {
    public static ApiErrorResponse of(int status, String errorCode, String message, String path) {
        return new ApiErrorResponse(false, status, errorCode, message, path, Instant.now(), List.of());
    }

    public static ApiErrorResponse of(int status, String errorCode, String message, String path, List<ValidationErrorDto> errors) {
        return new ApiErrorResponse(false, status, errorCode, message, path, Instant.now(), errors != null ? errors : List.of());
    }
}
