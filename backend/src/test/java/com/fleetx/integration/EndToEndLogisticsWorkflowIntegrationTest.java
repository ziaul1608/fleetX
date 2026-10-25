package com.fleetx.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fleetx.auth.config.DemoIdentitySeeder;
import com.fleetx.auth.entity.Role;
import com.fleetx.auth.entity.User;
import com.fleetx.auth.repository.RoleRepository;
import com.fleetx.auth.repository.UserRepository;
import com.fleetx.drivers.service.DemoDriverTripSeeder;
import com.fleetx.fleet.service.DemoFleetSeeder;
import com.fleetx.shipments.service.DemoShipmentSeeder;
import com.fleetx.tracking.service.DemoTrackingSeeder;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class EndToEndLogisticsWorkflowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private com.fleetx.auth.service.MockEmailService mockEmailService;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private DemoIdentitySeeder demoIdentitySeeder;

    @Autowired
    private DemoFleetSeeder demoFleetSeeder;

    @Autowired
    private DemoDriverTripSeeder demoDriverTripSeeder;

    @Autowired
    private DemoShipmentSeeder demoShipmentSeeder;

    @Autowired
    private DemoTrackingSeeder demoTrackingSeeder;

    @BeforeEach
    void ensureAdminAndRoles() {
        Role adminRole = roleRepository.findByName("ROLE_ADMIN")
                .orElseGet(() -> roleRepository.save(new Role("ROLE_ADMIN", "System Administrator")));
        if (!userRepository.existsByEmail("e2e.admin@fleetx.local")) {
            User admin = new User(
                    "e2e.admin@fleetx.local",
                    passwordEncoder.encode("IntegrationPass123!"),
                    "E2E Integration Admin"
            );
            admin.setActive(true);
            admin.setRoles(Set.of(adminRole));
            userRepository.save(admin);
        }
    }

    @Test
    @DisplayName("End-to-End Logistics Workflow: Login -> Vehicle -> Fuel -> Driver -> Trip -> Shipment & Waypoints -> GPS Simulation -> Geofence Alert Ack -> Delivery & Trip Completion -> Reports & CSV -> Logout")
    void fullEndToEndLogisticsScenario() throws Exception {
        // 0. CSRF Bootstrap + Passwordless Email OTP Login
        MvcResult csrfResult = mockMvc.perform(get("/api/v1/auth/csrf"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn();
        Cookie xsrfCookie = csrfResult.getResponse().getCookie("XSRF-TOKEN");

        mockMvc.perform(post("/api/v1/auth/otp/send-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", "e2e.admin@fleetx.local"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        String otp = mockEmailService.getLastSentOtp("e2e.admin@fleetx.local")
                .orElseThrow(() -> new IllegalStateException("OTP was not dispatched to mock email service"));

        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/otp/verify-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", "e2e.admin@fleetx.local",
                                "otp", otp
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value("e2e.admin@fleetx.local"))
                .andReturn();

        MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession(false);
        assertThat(session).isNotNull();

        // Verify /api/v1/auth/me with active session
        mockMvc.perform(get("/api/v1/auth/me").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value("e2e.admin@fleetx.local"));

        // Verify mutating request WITHOUT CSRF token is rejected with 403
        mockMvc.perform(post("/api/v1/fleet/vehicles")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());

        // 1. Module 2: Register Vehicle & Log Fuel Records
        String vehicleJson = objectMapper.writeValueAsString(Map.ofEntries(
                Map.entry("vin", "1HGCR2F83HA990001"),
                Map.entry("licensePlate", "MH-12-E2E-9001"),
                Map.entry("make", "Volvo"),
                Map.entry("model", "FH16 Heavy Hauler"),
                Map.entry("year", 2026),
                Map.entry("type", "TRUCK"),
                Map.entry("fuelType", "DIESEL"),
                Map.entry("capacityKg", 28000.0),
                Map.entry("capacityVolumeM3", 55.0),
                Map.entry("initialOdometerKm", 5000.0),
                Map.entry("status", "AVAILABLE")
        ));

        MvcResult vehicleRes = mockMvc.perform(post("/api/v1/fleet/vehicles")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(vehicleJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.licensePlate").value("MH-12-E2E-9001"))
                .andReturn();

        long vehicleId = objectMapper.readTree(vehicleRes.getResponse().getContentAsString())
                .path("data").path("id").asLong();

        // Log two fuel records to compute fuel efficiency
        mockMvc.perform(post("/api/v1/fleet/vehicles/" + vehicleId + "/fuel-records")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "liters", 100.0,
                                "pricePerLiter", 92.0,
                                "odometerAtFueling", 5400.0,
                                "fuelStation", "E2E Highway Station 1"
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.efficiencyKmPerLiter").value(4.0));

        // 2. Module 3: Onboard Driver
        String driverJson = objectMapper.writeValueAsString(Map.of(
                "fullName", "Suresh Patil",
                "phoneNumber", "+91 98111 22334",
                "email", "suresh.patil@fleetx.local",
                "licenseNumber", "DL-E2E-2026-9001",
                "licenseCategory", "HEAVY",
                "licenseExpiryDate", LocalDate.now().plusYears(3).toString(),
                "medicalCertExpiryDate", LocalDate.now().plusYears(1).toString(),
                "experienceYears", 8,
                "availabilityStatus", "AVAILABLE"
        ));

        MvcResult driverRes = mockMvc.perform(post("/api/v1/trips/drivers")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(driverJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.licenseNumber").value("DL-E2E-2026-9001"))
                .andReturn();

        long driverId = objectMapper.readTree(driverRes.getResponse().getContentAsString())
                .path("data").path("id").asLong();

        // 3. Module 3: Create Trip & Start Trip (verifies VehicleLookupService cross-module lookup)
        String tripJson = objectMapper.writeValueAsString(Map.of(
                "vehicleId", vehicleId,
                "driverId", driverId,
                "originAddress", "Mumbai Port Trust Terminal",
                "destinationAddress", "Pune Chakan Industrial Area",
                "scheduledStartTime", Instant.now().plus(15, ChronoUnit.MINUTES).toString(),
                "scheduledEndTime", Instant.now().plus(4, ChronoUnit.HOURS).toString(),
                "distanceKm", 152.0,
                "notes", "E2E integration test dispatch"
        ));

        MvcResult tripRes = mockMvc.perform(post("/api/v1/trips")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(tripJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("ASSIGNED"))
                .andExpect(jsonPath("$.data.vehicleLicensePlate").value("MH-12-E2E-9001"))
                .andReturn();

        JsonNode tripData = objectMapper.readTree(tripRes.getResponse().getContentAsString()).path("data");
        long tripId = tripData.path("id").asLong();
        String tripCode = tripData.path("tripCode").asText();
        assertThat(tripCode).startsWith("TRIP-");

        // Start Trip -> IN_PROGRESS
        mockMvc.perform(post("/api/v1/trips/" + tripId + "/start")
                        .session(session)
                        .with(csrf())
                        .param("remarks", "Departed Mumbai Port Terminal"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("IN_PROGRESS"));

        // 4. Module 4: Create Shipment, Plan Waypoints, Associate with Trip, Transition to IN_TRANSIT
        String shipmentJson = objectMapper.writeValueAsString(Map.of(
                "senderName", "Bharat Heavy Electricals",
                "senderAddress", "Mumbai Port Trust Terminal",
                "receiverName", "Chakan Auto Assembly Plant",
                "receiverAddress", "Pune Chakan MIDC Phase 2",
                "cargoDescription", "Industrial turbine assemblies",
                "weightKg", 4200.0,
                "volumeM3", 18.0,
                "promisedDeliveryTime", Instant.now().plus(5, ChronoUnit.HOURS).toString()
        ));

        MvcResult shipmentRes = mockMvc.perform(post("/api/v1/shipments")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(shipmentJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("CREATED"))
                .andReturn();

        JsonNode shipmentData = objectMapper.readTree(shipmentRes.getResponse().getContentAsString()).path("data");
        long shipmentId = shipmentData.path("id").asLong();
        String trackingNumber = shipmentData.path("trackingNumber").asText();
        assertThat(trackingNumber).startsWith("SHP-");

        // Plan Route Waypoints (Haversine distance & ETA estimate)
        String waypointsJson = objectMapper.writeValueAsString(Map.of(
                "waypoints", List.of(
                        Map.of("waypointName", "Mumbai Port Origin", "latitude", 18.9480, "longitude", 72.8440, "sequenceOrder", 1, "stopType", "ORIGIN"),
                        Map.of("waypointName", "Khalapur Toll Plaza", "latitude", 18.8250, "longitude", 73.2890, "sequenceOrder", 2, "stopType", "INTERMEDIATE"),
                        Map.of("waypointName", "Pune Chakan Destination", "latitude", 18.7600, "longitude", 73.8600, "sequenceOrder", 3, "stopType", "DESTINATION")
                )
        ));

        mockMvc.perform(put("/api/v1/shipments/" + shipmentId + "/waypoints")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(waypointsJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.estimatedDistanceKm").isNumber())
                .andExpect(jsonPath("$.data.distanceLabel").value(org.hamcrest.Matchers.containsString("Haversine")))
                .andExpect(jsonPath("$.data.etaLabel").value(org.hamcrest.Matchers.containsString("Estimated Arrival Time")));

        // Associate Shipment with Trip via TripLookupService
        mockMvc.perform(patch("/api/v1/shipments/" + shipmentId + "/assign-trip")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("tripId", tripId))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.tripId").value(tripId))
                .andExpect(jsonPath("$.data.tripCode").value(tripCode))
                .andExpect(jsonPath("$.data.status").value("ASSIGNED"));

        // Transition Shipment to IN_TRANSIT
        mockMvc.perform(patch("/api/v1/shipments/" + shipmentId + "/status")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "status", "IN_TRANSIT",
                                "locationName", "Panvel Expressway Entry",
                                "description", "In transit aboard " + tripCode
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("IN_TRANSIT"));

        // 5. Module 5: Geofence Creation, Simulated GPS Tracking, Alert Emission & Acknowledgement
        String geofenceJson = objectMapper.writeValueAsString(Map.of(
                "name", "Mumbai Port Origin Zone",
                "fenceType", "DEPOT",
                "shapeType", "CIRCLE",
                "centerLatitude", 18.9480,
                "centerLongitude", 72.8440,
                "radiusMeters", 5000.0,
                "speedLimitKmh", 50.0
        ));

        mockMvc.perform(post("/api/v1/tracking/geofences")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(geofenceJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.name").value("Mumbai Port Origin Zone"));

        // Start GPS Simulator for the Trip (automatically pulls waypoints from linked shipment via ShipmentLookupService)
        String simStartJson = objectMapper.writeValueAsString(Map.of(
                "vehicleId", vehicleId,
                "tripId", tripId,
                "tickIntervalSeconds", 5,
                "speedMultiplier", 1.0,
                "baseSpeedKmh", 72.0
        ));

        mockMvc.perform(post("/api/v1/tracking/simulator/start")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(simStartJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.vehicleId").value(vehicleId))
                .andExpect(jsonPath("$.data.simulated").value(true))
                .andExpect(jsonPath("$.data.dataSource").value(org.hamcrest.Matchers.containsString("SIMULATED")));

        // Deterministically step simulator forward and stop cleanly
        mockMvc.perform(post("/api/v1/tracking/simulator/" + vehicleId + "/step")
                        .session(session)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.simulatorStatus").value("RUNNING"));

        mockMvc.perform(post("/api/v1/tracking/simulator/" + vehicleId + "/stop")
                        .session(session)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.simulatorStatus").value("STOPPED"));

        // Check generated alerts and acknowledge the first unacknowledged alert
        MvcResult alertsRes = mockMvc.perform(get("/api/v1/tracking/alerts")
                        .session(session)
                        .param("acknowledged", "false"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode alertContent = objectMapper.readTree(alertsRes.getResponse().getContentAsString())
                .path("data").path("content");
        assertThat(alertContent.size()).isGreaterThanOrEqualTo(1);
        long firstAlertId = alertContent.get(0).path("id").asLong();

        mockMvc.perform(post("/api/v1/tracking/alerts/" + firstAlertId + "/acknowledge")
                        .session(session)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.acknowledged").value(true))
                .andExpect(jsonPath("$.data.acknowledgedBy").value("e2e.admin@fleetx.local"));

        // 6. Complete Shipment Delivery & Complete Trip
        mockMvc.perform(post("/api/v1/shipments/" + shipmentId + "/deliver")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "locationName", "Chakan Auto Assembly Receiving Dock",
                                "deliveryNotes", "Signed and received in good order"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("DELIVERED"))
                .andExpect(jsonPath("$.data.deliveredAt").isNotEmpty());

        mockMvc.perform(post("/api/v1/trips/" + tripId + "/complete")
                        .session(session)
                        .with(csrf())
                        .param("remarks", "Trip completed at Chakan MIDC"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("COMPLETED"));

        // Verify Driver Performance metrics updated after trip completion
        mockMvc.perform(get("/api/v1/trips/drivers/" + driverId + "/performance")
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalTripsCompleted").value(1));

        // 7. Verify Control Tower Summary, Filtered Persisted Reports & Safe CSV Export
        mockMvc.perform(get("/api/v1/tracking/control-tower/summary")
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.simulatedData").value(true))
                .andExpect(jsonPath("$.data.simulatedDataDisclaimer").value(org.hamcrest.Matchers.containsString("SIMULATED")))
                .andExpect(jsonPath("$.data.totalVehicles").isNumber());

        mockMvc.perform(get("/api/v1/tracking/reports/summary")
                        .session(session)
                        .param("reportType", "SHIPMENTS")
                        .param("status", "DELIVERED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.reportType").value("SHIPMENTS"))
                .andExpect(jsonPath("$.data.rows").isArray())
                .andExpect(jsonPath("$.data.rows[0].tracking_number").value(trackingNumber));

        MvcResult csvRes = mockMvc.perform(get("/api/v1/tracking/reports/export-csv")
                        .session(session)
                        .param("reportType", "SHIPMENTS")
                        .param("status", "DELIVERED"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("fleetx_shipments_report.csv")))
                .andReturn();

        String csvBody = csvRes.getResponse().getContentAsString();
        assertThat(csvBody).contains("tracking_number,sender_name,receiver_name");
        assertThat(csvBody).contains(trackingNumber);

        // 8. Verify Role-Based Authorization (VIEWER can read reports, cannot create vehicles; DISPATCHER cannot access /admin/users)
        mockMvc.perform(get("/api/v1/tracking/reports/summary")
                        .with(user("viewer@fleetx.local").roles("VIEWER")))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/fleet/vehicles")
                        .with(user("viewer@fleetx.local").roles("VIEWER"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(vehicleJson))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/admin/users")
                        .with(user("dispatcher@fleetx.local").roles("DISPATCHER")))
                .andExpect(status().isForbidden());

        // 9. Session Logout invalidates session
        mockMvc.perform(post("/api/v1/auth/logout")
                        .session(session)
                        .with(csrf()))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/auth/me").session(session))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Development demo dataset seeders are repeatable and idempotent across all 5 modules")
    void repeatableDemoDatasetSeedingIsIdempotent() {
        // First execution
        demoIdentitySeeder.seedDemoUsers();
        demoFleetSeeder.seedDemoFleet();
        demoDriverTripSeeder.seedDemoDriversAndTrips();
        demoShipmentSeeder.seedDemoShipments();
        demoTrackingSeeder.seedDemoGeofencesAndSimulation();

        long userCountAfterFirst = userRepository.count();

        // Second execution (simulating application restart) must not duplicate or fail
        demoIdentitySeeder.seedDemoUsers();
        demoFleetSeeder.seedDemoFleet();
        demoDriverTripSeeder.seedDemoDriversAndTrips();
        demoShipmentSeeder.seedDemoShipments();
        demoTrackingSeeder.seedDemoGeofencesAndSimulation();

        assertThat(userRepository.count()).isEqualTo(userCountAfterFirst);
    }
}
