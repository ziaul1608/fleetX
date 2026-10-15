package com.fleetx.shared.contract;

import com.fleetx.shared.dto.DriverSummaryDto;
import java.util.Optional;

public interface DriverLookupService {
    /**
     * Retrieves driver profile and compliance credentials.
     * @param driverId Unique database identifier of the driver.
     * @return Optional containing driver profile summary.
     */
    Optional<DriverSummaryDto> findDriverSummaryById(Long driverId);

    /**
     * Checks if driver is available and holds a valid, non-expired license.
     * @param driverId Unique database identifier.
     * @return true if available and certified; false otherwise.
     */
    boolean isDriverEligibleForTrip(Long driverId);

    /**
     * Sets the operational availability of a driver (e.g., ON_DUTY, AVAILABLE).
     * @param driverId Unique identifier.
     * @param status Target availability status string.
     */
    void setDriverAvailability(Long driverId, String status);
}
