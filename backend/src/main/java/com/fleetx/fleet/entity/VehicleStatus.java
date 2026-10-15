package com.fleetx.fleet.entity;

import com.fleetx.shared.exception.DomainException;
import org.springframework.http.HttpStatus;

import java.util.Arrays;

public enum VehicleStatus {
    ACTIVE,
    AVAILABLE,
    IDLE,
    IN_TRANSIT,
    IN_TRIP,
    MAINTENANCE,
    OFFLINE,
    RETIRED;

    public static VehicleStatus fromString(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            throw new DomainException("Vehicle status cannot be blank.", HttpStatus.BAD_REQUEST, "INVALID_VEHICLE_STATUS");
        }
        String normalized = raw.trim().toUpperCase().replace("-", "_").replace(" ", "_");
        for (VehicleStatus status : values()) {
            if (status.name().equals(normalized)) {
                return status;
            }
        }
        throw new DomainException("Invalid vehicle status '" + raw + "'. Allowed statuses: " + Arrays.toString(values()),
                HttpStatus.BAD_REQUEST, "INVALID_VEHICLE_STATUS");
    }
}
