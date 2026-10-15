package com.fleetx.fleet.service;

import com.fleetx.fleet.dto.FuelRecordCreateRequest;
import com.fleetx.fleet.dto.MaintenanceRecordCreateRequest;
import com.fleetx.fleet.dto.VehicleCreateRequest;
import com.fleetx.fleet.dto.VehicleResponseDto;
import com.fleetx.fleet.repository.VehicleRepository;
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

@Component
@Order(20)
public class DemoFleetSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoFleetSeeder.class);

    private final VehicleService vehicleService;
    private final FuelService fuelService;
    private final MaintenanceService maintenanceService;
    private final VehicleRepository vehicleRepository;

    @Value("${fleetx.demo.seed.enabled:false}")
    private boolean demoSeedEnabled;

    public DemoFleetSeeder(
            VehicleService vehicleService,
            FuelService fuelService,
            MaintenanceService maintenanceService,
            VehicleRepository vehicleRepository
    ) {
        this.vehicleService = vehicleService;
        this.fuelService = fuelService;
        this.maintenanceService = maintenanceService;
        this.vehicleRepository = vehicleRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!demoSeedEnabled) {
            return;
        }
        seedDemoFleet();
    }

    @Transactional
    public void seedDemoFleet() {
        if (!vehicleRepository.existsByLicensePlate("MH-12-FX-1001")) {
            VehicleResponseDto v1 = vehicleService.createVehicle(new VehicleCreateRequest(
                    "1HGCR2F83HA001001",
                    "MH-12-FX-1001",
                    "Tata",
                    "Prima 4028.S",
                    2025,
                    "TRUCK",
                    "DIESEL",
                    25000.0,
                    48.0,
                    12000.0,
                    "AVAILABLE"
            ));

            fuelService.recordFueling(v1.id(), new FuelRecordCreateRequest(
                    120.0,
                    new BigDecimal("92.50"),
                    null,
                    12420.0,
                    "HP Highway Plaza, Khalapur",
                    Instant.now().minus(3, ChronoUnit.DAYS)
            ));

            fuelService.recordFueling(v1.id(), new FuelRecordCreateRequest(
                    110.0,
                    new BigDecimal("92.50"),
                    null,
                    12860.0,
                    "IndianOil Expressway Hub, Talegaon",
                    Instant.now().minus(1, ChronoUnit.DAYS)
            ));

            maintenanceService.recordMaintenance(v1.id(), new MaintenanceRecordCreateRequest(
                    "PREVENTIVE",
                    "Scheduled 15,000 km multi-axle brake & oil inspection",
                    new BigDecimal("14500.00"),
                    LocalDate.now().minusDays(15),
                    12100.0,
                    13100.0,
                    LocalDate.now().plusDays(45),
                    "COMPLETED"
            ));
        }

        if (!vehicleRepository.existsByLicensePlate("MH-14-FX-2002")) {
            vehicleService.createVehicle(new VehicleCreateRequest(
                    "1HGCR2F83HA002002",
                    "MH-14-FX-2002",
                    "Ashok Leyland",
                    "Ecomet 1615 HE",
                    2024,
                    "TRUCK",
                    "DIESEL",
                    16000.0,
                    32.0,
                    8500.0,
                    "AVAILABLE"
            ));
        }

        log.info("[DemoFleetSeeder] Repeatable development fleet assets, fuel logs, and maintenance verified.");
    }
}
