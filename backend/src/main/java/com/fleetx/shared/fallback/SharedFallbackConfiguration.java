package com.fleetx.shared.fallback;

import com.fleetx.shared.contract.*;
import com.fleetx.shared.dto.DriverSummaryDto;
import com.fleetx.shared.dto.ShipmentSummaryDto;
import com.fleetx.shared.dto.TripSummaryDto;
import com.fleetx.shared.dto.VehicleSummaryDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Provides compile-safe, non-intrusive fallback implementations for shared module interfaces.
 * These fallbacks enable student developers to build and test their own domain branches
 * without requiring all other modules to be merged first.
 *
 * Rule: They do NOT generate fake business data and are superseded as soon as real module
 * providers are registered.
 */
@Configuration
public class SharedFallbackConfiguration {

    private static final Logger log = LoggerFactory.getLogger(SharedFallbackConfiguration.class);

    @Bean
    @ConditionalOnMissingBean(VehicleLookupService.class)
    public VehicleLookupService fallbackVehicleLookupService() {
        return new VehicleLookupService() {
            @Override
            public Optional<VehicleSummaryDto> findVehicleSummaryById(Long vehicleId) {
                log.debug("[Fallback] VehicleLookupService: findVehicleSummaryById({}) -> not implemented", vehicleId);
                return Optional.empty();
            }

            @Override
            public boolean isVehicleAvailableForTrip(Long vehicleId) {
                log.debug("[Fallback] VehicleLookupService: isVehicleAvailableForTrip({}) -> false", vehicleId);
                return false;
            }

            @Override
            public void updateVehicleOperationalStatus(Long vehicleId, String status) {
                log.debug("[Fallback] VehicleLookupService: updateVehicleOperationalStatus({}, {}) -> no-op", vehicleId, status);
            }
        };
    }

    @Bean
    @ConditionalOnMissingBean(DriverLookupService.class)
    public DriverLookupService fallbackDriverLookupService() {
        return new DriverLookupService() {
            @Override
            public Optional<DriverSummaryDto> findDriverSummaryById(Long driverId) {
                log.debug("[Fallback] DriverLookupService: findDriverSummaryById({}) -> not implemented", driverId);
                return Optional.empty();
            }

            @Override
            public boolean isDriverEligibleForTrip(Long driverId) {
                log.debug("[Fallback] DriverLookupService: isDriverEligibleForTrip({}) -> false", driverId);
                return false;
            }

            @Override
            public void setDriverAvailability(Long driverId, String status) {
                log.debug("[Fallback] DriverLookupService: setDriverAvailability({}, {}) -> no-op", driverId, status);
            }
        };
    }

    @Bean
    @ConditionalOnMissingBean(TripLookupService.class)
    public TripLookupService fallbackTripLookupService() {
        return new TripLookupService() {
            @Override
            public Optional<TripSummaryDto> findTripSummaryById(Long tripId) {
                log.debug("[Fallback] TripLookupService: findTripSummaryById({}) -> not implemented", tripId);
                return Optional.empty();
            }

            @Override
            public boolean isTripActive(Long tripId) {
                log.debug("[Fallback] TripLookupService: isTripActive({}) -> false", tripId);
                return false;
            }
        };
    }

    @Bean
    @ConditionalOnMissingBean(ShipmentLookupService.class)
    public ShipmentLookupService fallbackShipmentLookupService() {
        return new ShipmentLookupService() {
            @Override
            public Optional<ShipmentSummaryDto> findByTrackingNumber(String trackingNumber) {
                log.debug("[Fallback] ShipmentLookupService: findByTrackingNumber({}) -> not implemented", trackingNumber);
                return Optional.empty();
            }

            @Override
            public List<ShipmentSummaryDto> findShipmentsByTripId(Long tripId) {
                log.debug("[Fallback] ShipmentLookupService: findShipmentsByTripId({}) -> empty", tripId);
                return Collections.emptyList();
            }

            @Override
            public void recordTripMilestoneForShipments(Long tripId, String locationSummary, String eventType) {
                log.debug("[Fallback] ShipmentLookupService: recordTripMilestoneForShipments({}, {}, {}) -> no-op",
                        tripId, locationSummary, eventType);
            }
        };
    }

    @Bean
    @ConditionalOnMissingBean(AlertPublisher.class)
    public AlertPublisher fallbackAlertPublisher() {
        return (alertType, severity, entityType, entityId, description, timestamp) -> {
            log.info("[Fallback Alert] Type: {} | Severity: {} | Entity: {} #{} | Details: {} | At: {}",
                    alertType, severity, entityType, entityId, description, timestamp != null ? timestamp : Instant.now());
        };
    }
}
