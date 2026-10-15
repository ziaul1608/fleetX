package com.fleetx.shipments.dto;

import com.fasterxml.jackson.annotation.JsonAlias;

public record ShipmentDeliverRequest(
        String deliveryNotes,
        @JsonAlias({"locationName", "locationSummary"})
        String locationSummary
) {
}
