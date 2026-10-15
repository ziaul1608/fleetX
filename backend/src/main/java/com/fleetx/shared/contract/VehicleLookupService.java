package com.fleetx.shared.contract;

import com.fleetx.shared.dto.VehicleSummaryDto;
import java.util.Optional;

public interface VehicleLookupService {
    /**
     * Retrieves high-level operational status and specifications for a vehicle.
     * @param vehicleId Unique database identifier of the vehicle.
     * @return Optional containing immutable summary, or empty if not found.
     */
    Optional<VehicleSummaryDto> findVehicleSummaryById(Long vehicleId);

    /**
     * Validates whether a vehicle is currently eligible and safe for trip assignment.
     * @param vehicleId Unique database identifier.
     * @return true if vehicle is AVAILABLE and has no critical service warnings; false otherwise.
     */
    boolean isVehicleAvailableForTrip(Long vehicleId);

    /**
     * Updates the operational status of a vehicle upon trip progression.
     * @param vehicleId Unique identifier.
     * @param status Target status string ("AVAILABLE", "IN_TRIP", "MAINTENANCE").
     */
    void updateVehicleOperationalStatus(Long vehicleId, String status);
}
