package com.fleetx.shipments.service;

import com.fleetx.shared.contract.TripLookupService;
import com.fleetx.shared.dto.PageResponse;
import com.fleetx.shared.dto.TripSummaryDto;
import com.fleetx.shipments.dto.ShipmentCreateRequest;
import com.fleetx.shipments.dto.ShipmentResponseDto;
import com.fleetx.shipments.dto.WaypointItemRequest;
import com.fleetx.shipments.repository.ShipmentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Component
@Order(40)
public class DemoShipmentSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoShipmentSeeder.class);

    private final ShipmentService shipmentService;
    private final ShipmentRepository shipmentRepository;
    private final TripLookupService tripLookupService;

    @Value("${fleetx.demo.seed.enabled:false}")
    private boolean demoSeedEnabled;

    public DemoShipmentSeeder(
            ShipmentService shipmentService,
            ShipmentRepository shipmentRepository,
            TripLookupService tripLookupService
    ) {
        this.shipmentService = shipmentService;
        this.shipmentRepository = shipmentRepository;
        this.tripLookupService = tripLookupService;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!demoSeedEnabled) {
            return;
        }
        seedDemoShipments();
    }

    @Transactional
    public void seedDemoShipments() {
        if (shipmentRepository.existsByTrackingNumber("SHP-2026-9001")) {
            return;
        }

        Long activeTripId = null;
        for (long candidateTripId = 1L; candidateTripId <= 10L; candidateTripId++) {
            var tripOpt = tripLookupService.findTripSummaryById(candidateTripId);
            if (tripOpt.isPresent()) {
                TripSummaryDto t = tripOpt.get();
                if ("IN_PROGRESS".equalsIgnoreCase(t.status()) || "ASSIGNED".equalsIgnoreCase(t.status()) || "IN_TRANSIT".equalsIgnoreCase(t.status())) {
                    activeTripId = t.id();
                    break;
                }
            }
        }

        ShipmentResponseDto created = shipmentService.createShipment(new ShipmentCreateRequest(
                "SHP-2026-9001",
                "Acme Semiconductor Systems Mumbai",
                "Mumbai Bhiwandi Logistics Hub, Thane",
                "TechHub Enterprise Park Pune",
                "Hinjawadi Phase 2, Pune",
                "Precision server racks and power distribution units",
                2450.0,
                14.5,
                Instant.now().plus(4, ChronoUnit.HOURS),
                activeTripId
        ));

        shipmentService.saveWaypointsAndCalculateRoute(created.id(), List.of(
                new WaypointItemRequest("Mumbai Bhiwandi Hub (Origin)", 19.2968, 73.0631, 1, "ORIGIN"),
                new WaypointItemRequest("Lonavala Expressway Checkpoint", 18.7557, 73.4091, 2, "INTERMEDIATE"),
                new WaypointItemRequest("Pune Hinjawadi Phase 2 (Destination)", 18.5913, 73.7389, 3, "DESTINATION")
        ));

        if (activeTripId != null) {
            shipmentService.updateShipmentStatus(
                    created.id(),
                    "IN_TRANSIT",
                    "Mumbai-Pune Expressway Corridor",
                    "Consignment departed Bhiwandi origin hub aboard assigned trip."
            );
        }

        log.info("[DemoShipmentSeeder] Repeatable development shipment manifest and Haversine waypoints verified.");
    }
}
