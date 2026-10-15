package com.fleetx.fleet.entity;

import com.fleetx.shared.exception.DomainException;
import org.springframework.http.HttpStatus;
import java.util.Arrays;

public enum VehicleType {
    TRUCK,
    VAN,
    CONTAINER,
    MOTORCYCLE,
    TRAILER;

    public static VehicleType fromString(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            throw new DomainException("Vehicle type cannot be blank.", HttpStatus.BAD_REQUEST, "INVALID_VEHICLE_TYPE");
        }
        String normalized = raw.trim().toUpperCase();
        for (VehicleType type : values()) {
            if (type.name().equals(normalized)) {
                return type;
            }
        }
        throw new DomainException("Invalid vehicle type '" + raw + "'. Allowed types: " + Arrays.toString(values()),
                HttpStatus.BAD_REQUEST, "INVALID_VEHICLE_TYPE");
    }
}
