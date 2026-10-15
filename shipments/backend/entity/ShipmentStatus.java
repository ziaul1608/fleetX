package com.fleetx.shipments.entity;

public enum ShipmentStatus {
    CREATED,
    ASSIGNED,
    PICKED_UP,
    IN_TRANSIT,
    OUT_FOR_DELIVERY,
    DELIVERED,
    CANCELLED,
    DELAYED;

    public static ShipmentStatus fromString(String status) {
        if (status == null || status.isBlank()) {
            return CREATED;
        }
        String clean = status.trim().toUpperCase();
        // Support legacy aliases from baseline
        if ("ORDERED".equals(clean)) {
            return CREATED;
        }
        if ("DISPATCHED".equals(clean)) {
            return ASSIGNED;
        }
        if ("RETURNED".equals(clean)) {
            return CANCELLED;
        }
        for (ShipmentStatus val : values()) {
            if (val.name().equals(clean)) {
                return val;
            }
        }
        throw new IllegalArgumentException("Unknown shipment status: " + status);
    }

    public boolean isTerminal() {
        return this == DELIVERED || this == CANCELLED;
    }

    public boolean canTransitionTo(ShipmentStatus target) {
        if (this == target) {
            return true;
        }
        if (this.isTerminal()) {
            return false;
        }
        if (target == CANCELLED || target == DELAYED) {
            return true;
        }
        return switch (this) {
            case CREATED -> target == ASSIGNED || target == PICKED_UP;
            case ASSIGNED -> target == PICKED_UP || target == IN_TRANSIT;
            case PICKED_UP -> target == IN_TRANSIT || target == OUT_FOR_DELIVERY;
            case IN_TRANSIT -> target == OUT_FOR_DELIVERY || target == DELIVERED;
            case OUT_FOR_DELIVERY -> target == DELIVERED;
            case DELAYED -> target == ASSIGNED || target == PICKED_UP || target == IN_TRANSIT || target == OUT_FOR_DELIVERY || target == DELIVERED;
            default -> false;
        };
    }
}
