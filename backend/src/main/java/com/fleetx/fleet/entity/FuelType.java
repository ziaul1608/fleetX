package com.fleetx.fleet.entity;

import com.fleetx.shared.exception.DomainException;
import org.springframework.http.HttpStatus;
import java.util.Arrays;

public enum FuelType {
    DIESEL,
    PETROL,
    ELECTRIC,
    CNG,
    HYBRID;

    public static FuelType fromString(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            throw new DomainException("Fuel type cannot be blank.", HttpStatus.BAD_REQUEST, "INVALID_FUEL_TYPE");
        }
        String normalized = raw.trim().toUpperCase();
        for (FuelType fuel : values()) {
            if (fuel.name().equals(normalized)) {
                return fuel;
            }
        }
        throw new DomainException("Invalid fuel type '" + raw + "'. Allowed fuel types: " + Arrays.toString(values()),
                HttpStatus.BAD_REQUEST, "INVALID_FUEL_TYPE");
    }
}
