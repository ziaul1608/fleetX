package com.fleetx.shared.dto;

public record ValidationErrorDto(
        String field,
        Object rejectedValue,
        String message
) {
}
