package com.fleetx.fleet.entity;

import com.fleetx.shared.exception.DomainException;
import org.springframework.http.HttpStatus;
import java.util.Arrays;

public enum ServiceType {
    PREVENTIVE,
    REPAIR,
    INSPECTION;

    public static ServiceType fromString(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            throw new DomainException("Service type cannot be blank.", HttpStatus.BAD_REQUEST, "INVALID_SERVICE_TYPE");
        }
        String normalized = raw.trim().toUpperCase();
        for (ServiceType type : values()) {
            if (type.name().equals(normalized)) {
                return type;
            }
        }
        throw new DomainException("Invalid service type '" + raw + "'. Allowed types: " + Arrays.toString(values()),
                HttpStatus.BAD_REQUEST, "INVALID_SERVICE_TYPE");
    }
}
