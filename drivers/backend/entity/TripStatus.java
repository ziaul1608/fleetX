package com.fleetx.drivers.entity;

public enum TripStatus {
    PLANNED,
    ASSIGNED,
    IN_PROGRESS,
    COMPLETED,
    CANCELLED;

    public static TripStatus fromString(String status) {
        if (status == null || status.isBlank()) {
            return PLANNED;
        }
        String clean = status.trim().toUpperCase();
        // Handle common domain aliases
        if ("DRAFT".equals(clean)) {
            return PLANNED;
        }
        if ("IN_TRANSIT".equals(clean)) {
            return IN_PROGRESS;
        }
        for (TripStatus val : values()) {
            if (val.name().equals(clean)) {
                return val;
            }
        }
        throw new IllegalArgumentException("Unknown trip status: " + status);
    }

    public boolean isTerminal() {
        return this == COMPLETED || this == CANCELLED;
    }

    public boolean isActive() {
        return this == IN_PROGRESS;
    }
}
