package com.fleetx.shared.contract;

import com.fleetx.shared.dto.ShipmentSummaryDto;
import java.util.List;
import java.util.Optional;

public interface ShipmentLookupService {
    /**
     * Retrieves shipment details by business tracking number.
     * @param trackingNumber Unique string code (e.g., SHP-2026-1001).
     * @return Optional containing shipment summary.
     */
    Optional<ShipmentSummaryDto> findByTrackingNumber(String trackingNumber);

    /**
     * Retrieves all cargo shipments assigned to a specific active trip.
     * @param tripId Unique identifier of the trip.
     * @return List of immutable shipment summaries.
     */
    List<ShipmentSummaryDto> findShipmentsByTripId(Long tripId);

    /**
     * Appends an automated progress milestone to all shipments linked to a trip.
     * @param tripId Active trip identifier.
     * @param locationSummary Current GPS location or waypoint description.
     * @param eventType Milestone description (e.g., IN_TRANSIT, CHECKPOINT_REACHED).
     */
    void recordTripMilestoneForShipments(Long tripId, String locationSummary, String eventType);
}
