package com.fleetx.shared.contract;

import com.fleetx.shared.dto.TripSummaryDto;
import java.util.Optional;

public interface TripLookupService {
    /**
     * Retrieves current trip status, assigned vehicle ID, and driver ID.
     * @param tripId Unique database identifier of the trip.
     * @return Optional containing immutable trip details.
     */
    Optional<TripSummaryDto> findTripSummaryById(Long tripId);

    /**
     * Checks if a trip is currently active and eligible for real-time telemetry streaming.
     * @param tripId Unique database identifier.
     * @return true if status is IN_TRANSIT; false otherwise.
     */
    boolean isTripActive(Long tripId);

    /**
     * Checks if a vehicle is currently assigned to an active trip (ASSIGNED or IN_PROGRESS).
     * @param vehicleId Unique database identifier of the vehicle.
     * @return true if an active trip requires this vehicle; false otherwise.
     */
    boolean hasActiveTripForVehicle(Long vehicleId);
}
