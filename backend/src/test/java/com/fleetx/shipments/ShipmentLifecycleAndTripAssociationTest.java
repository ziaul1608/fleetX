package com.fleetx.shipments;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fleetx.FleetxApplication;
import com.fleetx.drivers.entity.Driver;
import com.fleetx.drivers.entity.Trip;
import com.fleetx.drivers.repository.DriverRepository;
import com.fleetx.drivers.repository.TripRepository;
import com.fleetx.fleet.entity.Vehicle;
import com.fleetx.fleet.repository.VehicleRepository;
import com.fleetx.shared.contract.ShipmentLookupService;
import com.fleetx.shared.dto.ShipmentSummaryDto;
import com.fleetx.shipments.dto.ShipmentAssignTripRequest;
import com.fleetx.shipments.dto.ShipmentCreateRequest;
import com.fleetx.shipments.dto.ShipmentDeliverRequest;
import com.fleetx.shipments.dto.ShipmentStatusUpdateRequest;
import com.fleetx.shipments.entity.Shipment;
import com.fleetx.shipments.repository.ShipmentRepository;
import com.fleetx.shipments.repository.ShipmentTimelineEventRepository;
import com.fleetx.shipments.repository.ShipmentWaypointRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = FleetxApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class ShipmentLifecycleAndTripAssociationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ShipmentRepository shipmentRepository;

    @Autowired
    private ShipmentWaypointRepository waypointRepository;

    @Autowired
    private ShipmentTimelineEventRepository timelineRepository;

    @Autowired
    private TripRepository tripRepository;

    @Autowired
    private VehicleRepository vehicleRepository;

    @Autowired
    private DriverRepository driverRepository;

    @Autowired
    private ShipmentLookupService shipmentLookupService;

    private Long testTripId;

    @BeforeEach
    void setupCleanState() {
        timelineRepository.deleteAll();
        waypointRepository.deleteAll();
        shipmentRepository.deleteAll();
        tripRepository.deleteAll();
        driverRepository.deleteAll();
        vehicleRepository.deleteAll();

        // Seed Vehicle, Driver, and Trip for association tests
        Vehicle v = new Vehicle();
        v.setVin("1HGCR2F83HA009999");
        v.setLicensePlate("MH-12-SHP-01");
        v.setMake("Tata");
        v.setModel("Prima 4028");
        v.setModelYear(2024);
        v.setVehicleType("TRUCK");
        v.setFuelType("DIESEL");
        v.setCapacityKg(25000.0);
        v.setCapacityVolumeM3(45.0);
        v.setOdometerKm(10000.0);
        v.setStatus("IN_TRIP");
        v.setActive(true);
        Vehicle savedV = vehicleRepository.save(v);

        Driver d = new Driver();
        d.setFullName("Rohan Kulkarni");
        d.setPhoneNumber("+91 98765 55555");
        d.setLicenseNumber("MH-14-ROHAN-55");
        d.setLicenseCategory("HEAVY");
        d.setLicenseExpiryDate(LocalDate.now().plusYears(3));
        d.setAvailabilityStatus("ON_DUTY");
        d.setActive(true);
        Driver savedD = driverRepository.save(d);

        Trip trip = new Trip();
        trip.setTripCode("TRIP-2026-5001");
        trip.setVehicleId(savedV.getId());
        trip.setDriverId(savedD.getId());
        trip.setOriginAddress("Bhiwandi Hub, Thane");
        trip.setDestinationAddress("Hinjawadi Phase 2, Pune");
        trip.setStatus("ASSIGNED");
        trip.setScheduledStartTime(Instant.now().plusSeconds(3600));
        Trip savedT = tripRepository.save(trip);
        testTripId = savedT.getId();
    }

    @Test
    @DisplayName("POST /api/v1/shipments creates shipment in CREATED status and logs initial timeline event")
    @WithMockUser(username = "dispatcher@fleetx.test", roles = {"DISPATCHER"})
    void createShipment_success() throws Exception {
        ShipmentCreateRequest req = new ShipmentCreateRequest(
                null,
                "Acme Electronics Ltd",
                "Bhiwandi Logistics Park, Thane",
                "Tech Retailers Pune",
                "Hinjawadi Phase 2, Pune",
                "Fragile Server Rack Components",
                1250.5,
                4.8,
                Instant.now().plusSeconds(86400),
                null
        );

        mockMvc.perform(post("/api/v1/shipments")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.senderName").value("Acme Electronics Ltd"))
                .andExpect(jsonPath("$.data.receiverName").value("Tech Retailers Pune"))
                .andExpect(jsonPath("$.data.status").value("CREATED"))
                .andExpect(jsonPath("$.data.trackingNumber").isNotEmpty());

        assertThat(timelineRepository.findAll()).hasSize(1);
    }

    @Test
    @DisplayName("POST /api/v1/shipments rejects duplicate tracking reference with 409 Conflict")
    @WithMockUser(username = "dispatcher@fleetx.test", roles = {"DISPATCHER"})
    void createShipment_duplicateTrackingNumber_returns409() throws Exception {
        Shipment existing = new Shipment();
        existing.setTrackingNumber("SHP-2026-DUP01");
        existing.setSenderName("Sender A");
        existing.setSenderAddress("Address A");
        existing.setReceiverName("Receiver B");
        existing.setReceiverAddress("Address B");
        existing.setWeightKg(500.0);
        existing.setVolumeM3(2.0);
        existing.setStatus("CREATED");
        shipmentRepository.save(existing);

        ShipmentCreateRequest req = new ShipmentCreateRequest(
                "SHP-2026-DUP01",
                "Sender C",
                "Address C",
                "Receiver D",
                "Address D",
                "Duplicate Test",
                300.0,
                1.5,
                null,
                null
        );

        mockMvc.perform(post("/api/v1/shipments")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("RESOURCE_CONFLICT"));
    }

    @Test
    @DisplayName("POST /api/v1/shipments rejects negative cargo weight with 400 Bad Request")
    @WithMockUser(username = "dispatcher@fleetx.test", roles = {"DISPATCHER"})
    void createShipment_negativeWeight_returns400() throws Exception {
        ShipmentCreateRequest req = new ShipmentCreateRequest(
                null,
                "Sender A",
                "Origin Hub",
                "Receiver B",
                "Dest Hub",
                "Negative weight cargo",
                -150.0,
                2.0,
                null,
                null
        );

        mockMvc.perform(post("/api/v1/shipments")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"));
    }

    @Test
    @DisplayName("Associating shipment with valid Trip transitions status to ASSIGNED")
    @WithMockUser(username = "dispatcher@fleetx.test", roles = {"DISPATCHER"})
    void assignTrip_validTrip_success() throws Exception {
        Shipment s = new Shipment();
        s.setTrackingNumber("SHP-2026-1001");
        s.setSenderName("Metro Pharma");
        s.setSenderAddress("Mumbai Hub");
        s.setReceiverName("City Care Hospital");
        s.setReceiverAddress("Pune Hub");
        s.setWeightKg(450.0);
        s.setVolumeM3(1.5);
        s.setStatus("CREATED");
        Shipment savedS = shipmentRepository.save(s);

        ShipmentAssignTripRequest assignReq = new ShipmentAssignTripRequest(testTripId);

        mockMvc.perform(post("/api/v1/shipments/" + savedS.getId() + "/assign-trip")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(assignReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.tripId").value(testTripId))
                .andExpect(jsonPath("$.data.tripCode").value("TRIP-2026-5001"))
                .andExpect(jsonPath("$.data.status").value("ASSIGNED"));
    }

    @Test
    @DisplayName("Associating shipment with non-existent Trip is rejected with 400 Bad Request")
    @WithMockUser(username = "dispatcher@fleetx.test", roles = {"DISPATCHER"})
    void assignTrip_nonExistentTrip_rejected() throws Exception {
        Shipment s = new Shipment();
        s.setTrackingNumber("SHP-2026-1002");
        s.setSenderName("Metro Pharma");
        s.setSenderAddress("Mumbai Hub");
        s.setReceiverName("City Care Hospital");
        s.setReceiverAddress("Pune Hub");
        s.setWeightKg(450.0);
        s.setVolumeM3(1.5);
        s.setStatus("CREATED");
        Shipment savedS = shipmentRepository.save(s);

        ShipmentAssignTripRequest assignReq = new ShipmentAssignTripRequest(999999L);

        mockMvc.perform(post("/api/v1/shipments/" + savedS.getId() + "/assign-trip")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(assignReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_TRIP_ASSOCIATION"));
    }

    @Test
    @DisplayName("Shipment lifecycle transitions legally: CREATED -> ASSIGNED -> PICKED_UP -> IN_TRANSIT -> DELIVERED")
    @WithMockUser(username = "dispatcher@fleetx.test", roles = {"DISPATCHER"})
    void shipmentLifecycle_legalProgression_success() throws Exception {
        Shipment s = new Shipment();
        s.setTrackingNumber("SHP-2026-1003");
        s.setSenderName("Alpha Goods");
        s.setSenderAddress("Mumbai Hub");
        s.setReceiverName("Beta Store");
        s.setReceiverAddress("Pune Hub");
        s.setWeightKg(800.0);
        s.setVolumeM3(3.2);
        s.setStatus("CREATED");
        Shipment saved = shipmentRepository.save(s);

        // 1. ASSIGNED
        mockMvc.perform(put("/api/v1/shipments/" + saved.getId() + "/status")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ShipmentStatusUpdateRequest("ASSIGNED", "Mumbai Hub", "Assigned to dispatch"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ASSIGNED"));

        // 2. PICKED_UP
        mockMvc.perform(put("/api/v1/shipments/" + saved.getId() + "/status")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ShipmentStatusUpdateRequest("PICKED_UP", "Mumbai Hub", "Loaded onto truck"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PICKED_UP"));

        // 3. IN_TRANSIT
        mockMvc.perform(put("/api/v1/shipments/" + saved.getId() + "/status")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ShipmentStatusUpdateRequest("IN_TRANSIT", "Lonavala Toll", "In transit on expressway"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("IN_TRANSIT"));

        // 4. Complete delivery via /deliver endpoint
        mockMvc.perform(post("/api/v1/shipments/" + saved.getId() + "/deliver")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ShipmentDeliverRequest("Received by warehouse manager", "Pune Hub"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("DELIVERED"))
                .andExpect(jsonPath("$.data.deliveredAt").isNotEmpty())
                .andExpect(jsonPath("$.data.deliveryNotes").value("Received by warehouse manager"));

        // Verify timeline has all 4 transition events
        mockMvc.perform(get("/api/v1/shipments/SHP-2026-1003/timeline"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(4));
    }

    @Test
    @DisplayName("Illegal status transition from CREATED directly to DELIVERED is rejected with 400 Bad Request")
    @WithMockUser(username = "dispatcher@fleetx.test", roles = {"DISPATCHER"})
    void shipmentLifecycle_illegalDirectDelivery_rejected() throws Exception {
        Shipment s = new Shipment();
        s.setTrackingNumber("SHP-2026-1004");
        s.setSenderName("Alpha Goods");
        s.setSenderAddress("Mumbai Hub");
        s.setReceiverName("Beta Store");
        s.setReceiverAddress("Pune Hub");
        s.setWeightKg(800.0);
        s.setVolumeM3(3.2);
        s.setStatus("CREATED");
        Shipment saved = shipmentRepository.save(s);

        mockMvc.perform(put("/api/v1/shipments/" + saved.getId() + "/status")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ShipmentStatusUpdateRequest("DELIVERED", "Pune Hub", "Premature delivery attempt"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_STATE_TRANSITION"));
    }

    @Test
    @DisplayName("ShipmentLookupService contract methods function accurately for Tracking module")
    void shipmentLookupService_contractVerification() {
        Shipment s = new Shipment();
        s.setTrackingNumber("SHP-2026-1005");
        s.setTripId(testTripId);
        s.setSenderName("Contract Sender");
        s.setSenderAddress("Origin");
        s.setReceiverName("Contract Receiver");
        s.setReceiverAddress("Dest");
        s.setWeightKg(650.0);
        s.setVolumeM3(2.5);
        s.setStatus("IN_TRANSIT");
        Shipment saved = shipmentRepository.save(s);

        Optional<ShipmentSummaryDto> byTracking = shipmentLookupService.findByTrackingNumber("SHP-2026-1005");
        assertThat(byTracking).isPresent();
        assertThat(byTracking.get().weightKg()).isEqualTo(650.0);

        List<ShipmentSummaryDto> byTrip = shipmentLookupService.findShipmentsByTripId(testTripId);
        assertThat(byTrip).hasSize(1);

        shipmentLookupService.recordTripMilestoneForShipments(testTripId, "Khandala Ghat Checkpoint", "CHECKPOINT_REACHED");
        assertThat(timelineRepository.findByShipmentIdOrderByCreatedAtAsc(saved.getId())).hasSize(1);
    }
}
