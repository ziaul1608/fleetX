package com.fleetx.drivers.entity;

public enum DriverAvailability {
    AVAILABLE,
    ON_DUTY,
    SUSPENDED,
    ON_LEAVE,
    INACTIVE;

    public static DriverAvailability fromString(String status) {
        if (status == null || status.isBlank()) {
            return AVAILABLE;
        }
        for (DriverAvailability val : values()) {
            if (val.name().equalsIgnoreCase(status.trim())) {
                return val;
            }
        }
        throw new IllegalArgumentException("Unknown driver availability status: " + status);
    }
}
