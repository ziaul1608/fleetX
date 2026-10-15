package com.fleetx.drivers.service;

import com.fleetx.drivers.dto.DriverCreateRequest;
import com.fleetx.drivers.dto.DriverResponseDto;
import com.fleetx.drivers.dto.TripCreateRequest;
import com.fleetx.drivers.dto.TripResponseDto;
import com.fleetx.drivers.repository.DriverRepository;
import com.fleetx.drivers.repository.TripRepository;
import com.fleetx.shared.contract.VehicleLookupService;
import com.fleetx.shared.dto.VehicleSummaryDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Component
@Order(30)
public class DemoDriverTripSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDriverTripSeeder.class);

    private final DriverService driverService;
    private final TripService tripService;
    private final DriverRepository driverRepository;
    private final TripRepository tripRepository;
    private final VehicleLookupService vehicleLookupService;

    @Value("${fleetx.demo.seed.enabled:false}")
    private boolean demoSeedEnabled;

    public DemoDriverTripSeeder(
            DriverService driverService,
            TripService tripService,
            DriverRepository driverRepository,
            TripRepository tripRepository,
            VehicleLookupService vehicleLookupService
    ) {
        this.driverService = driverService;
        this.tripService = tripService;
        this.driverRepository = driverRepository;
        this.tripRepository = tripRepository;
        this.vehicleLookupService = vehicleLookupService;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!demoSeedEnabled) {
            return;
        }
        seedDemoDriversAndTrips();
    }

    @Transactional
    public void seedDemoDriversAndTrips() {
        DriverResponseDto d1;
        var existingD1 = driverRepository.findByLicenseNumber("DL-MH1220260001");
        if (existingD1.isEmpty()) {
            d1 = driverService.createDriver(new DriverCreateRequest(
                    null,
                    "Rajesh Sharma",
                    "+91 98220 11001",
                    "driver.rajesh@fleetx.local",
                    "DL-MH1220260001",
                    "HEAVY",
                    LocalDate.now().plusYears(3),
                    LocalDate.now().plusYears(1),
                    9,
                    "AVAILABLE",
                    "Certified hazmat & expressway heavy commercial driver"
            ));
        } else {
            d1 = driverService.getDriverById(existingD1.get().getId());
        }

        if (driverRepository.findByLicenseNumber("DL-MH1420260002").isEmpty()) {
            driverService.createDriver(new DriverCreateRequest(
                    null,
                    "Vikram Deshmukh",
                    "+91 98220 22002",
                    "driver.vikram@fleetx.local",
                    "DL-MH1420260002",
                    "HEAVY",
                    LocalDate.now().plusYears(2),
                    LocalDate.now().plusMonths(9),
                    6,
                    "AVAILABLE",
                    "Regional Maharashtra corridor operator"
            ));
        }

        if (tripRepository.count() == 0 && "AVAILABLE".equalsIgnoreCase(d1.availabilityStatus())) {
            for (long candidateVehicleId = 1L; candidateVehicleId <= 10L; candidateVehicleId++) {
                var vehicleOpt = vehicleLookupService.findVehicleSummaryById(candidateVehicleId);
                if (vehicleOpt.isPresent() && vehicleLookupService.isVehicleAvailableForTrip(candidateVehicleId)) {
                    VehicleSummaryDto primaryVehicle = vehicleOpt.get();
                    TripResponseDto trip = tripService.createTrip(new TripCreateRequest(
                            primaryVehicle.id(),
                            d1.id(),
                            "Mumbai Bhiwandi Logistics Hub, NH-48",
                            "Pune Hinjawadi Phase 2 Distribution Center",
                            Instant.now().minus(1, ChronoUnit.HOURS),
                            Instant.now().plus(3, ChronoUnit.HOURS),
                            new BigDecimal("148.50"),
                            "Express electronics corridor dispatch"
                    ));
                    tripService.updateTripStatus(trip.id(), "IN_PROGRESS", "Departed Bhiwandi Hub on schedule.", null);
                    break;
                }
            }
        }

        log.info("[DemoDriverTripSeeder] Repeatable development drivers and trip dispatches verified.");
    }
}
