package com.fleetx.shipments.entity;

public enum StopType {
    ORIGIN,
    INTERMEDIATE,
    DESTINATION;

    public static StopType fromString(String type) {
        if (type == null || type.isBlank()) {
            return INTERMEDIATE;
        }
        String clean = type.trim().toUpperCase();
        for (StopType val : values()) {
            if (val.name().equals(clean)) {
                return val;
            }
        }
        throw new IllegalArgumentException("Unknown waypoint stop type: " + type);
    }
}
