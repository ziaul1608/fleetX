package com.fleetx.tracking.service;

import com.fleetx.shared.contract.TripLookupService;
import com.fleetx.shared.dto.PageResponse;
import com.fleetx.shared.dto.TripSummaryDto;
import com.fleetx.tracking.dto.GeofenceCreateRequest;
import com.fleetx.tracking.dto.SimulatorStartRequest;
import com.fleetx.tracking.dto.SimulatorWaypointDto;
import com.fleetx.tracking.repository.GeofenceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Order(50)
public class DemoTrackingSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoTrackingSeeder.class);

    private final TrackingService trackingService;
    private final GeofenceRepository geofenceRepository;
    private final TripLookupService tripLookupService;

    @Value("${fleetx.demo.seed.enabled:false}")
    private boolean demoSeedEnabled;

    public DemoTrackingSeeder(
            TrackingService trackingService,
            GeofenceRepository geofenceRepository,
            TripLookupService tripLookupService
    ) {
        this.trackingService = trackingService;
        this.geofenceRepository = geofenceRepository;
        this.tripLookupService = tripLookupService;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!demoSeedEnabled) {
            return;
        }
        seedDemoGeofencesAndSimulation();
    }

    public void seedDemoGeofencesAndSimulation() {
        if (!geofenceRepository.existsByNameIgnoreCase("Mumbai Bhiwandi Origin Depot")) {
            trackingService.createGeofence(new GeofenceCreateRequest(
                    "Mumbai Bhiwandi Origin Depot",
                    19.2968,
                    73.0631,
                    800.0,
                    "DEPOT",
                    "CIRCLE",
                    null,
                    65.0
            ));
        }

        if (!geofenceRepository.existsByNameIgnoreCase("Pune Hinjawadi Delivery Zone")) {
            trackingService.createGeofence(new GeofenceCreateRequest(
                    "Pune Hinjawadi Delivery Zone",
                    18.5913,
                    73.7389,
                    500.0,
                    "CUSTOMER_HUB",
                    "POLYGON",
                    List.of(
                            List.of(18.6050, 73.7250),
                            List.of(18.6050, 73.7550),
                            List.of(18.5750, 73.7550),
                            List.of(18.5750, 73.7250)
                    ),
                    60.0
            ));
        }

        if (trackingService.getSimulatorSessions().isEmpty()) {
            for (long candidateTripId = 1L; candidateTripId <= 10L; candidateTripId++) {
                var tripOpt = tripLookupService.findTripSummaryById(candidateTripId);
                if (tripOpt.isPresent()) {
                    TripSummaryDto trip = tripOpt.get();
                    if ("IN_PROGRESS".equalsIgnoreCase(trip.status()) || "ASSIGNED".equalsIgnoreCase(trip.status()) || "IN_TRANSIT".equalsIgnoreCase(trip.status())) {
                        SimulatorStartRequest req = new SimulatorStartRequest(
                                trip.vehicleId(),
                                trip.id(),
                                5,
                                1.0,
                                58.0,
                                List.of(
                                        new SimulatorWaypointDto(19.2968, 73.0631, "Mumbai Bhiwandi Hub (Origin)"),
                                        new SimulatorWaypointDto(18.7557, 73.4091, "Lonavala Expressway Checkpoint"),
                                        new SimulatorWaypointDto(18.5913, 73.7389, "Pune Hinjawadi Phase 2 (Destination)")
                                )
                        );
                        trackingService.startSimulation(req);
                        trackingService.pauseSimulation(trip.vehicleId());
                        break;
                    }
                }
            }
        }

        log.info("[DemoTrackingSeeder] Repeatable development geofences and simulated GPS session verified.");
    }
}
