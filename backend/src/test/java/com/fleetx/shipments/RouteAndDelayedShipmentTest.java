package com.fleetx.shipments;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fleetx.FleetxApplication;
import com.fleetx.shared.contract.AlertPublisher;
import com.fleetx.shipments.dto.WaypointItemRequest;
import com.fleetx.shipments.dto.WaypointsUpdateRequest;
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
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThan;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = FleetxApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class RouteAndDelayedShipmentTest {

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

    @SpyBean
    private AlertPublisher alertPublisher;

    private Long testShipmentId;

    @BeforeEach
    void setupCleanState() {
        timelineRepository.deleteAll();
        waypointRepository.deleteAll();
        shipmentRepository.deleteAll();

        Shipment s = new Shipment();
        s.setTrackingNumber("SHP-2026-ROUTE01");
        s.setSenderName("Mumbai Logistics");
        s.setSenderAddress("Bhiwandi Hub, Thane");
        s.setReceiverName("Pune Tech Park");
        s.setReceiverAddress("Hinjawadi Phase 2, Pune");
        s.setWeightKg(1500.0);
        s.setVolumeM3(6.0);
        s.setStatus("CREATED");
        s.setPromisedDeliveryTime(Instant.now().plusSeconds(86400)); // +24h
        Shipment saved = shipmentRepository.save(s);
        testShipmentId = saved.getId();
    }

    @Test
    @DisplayName("POST /api/v1/shipments/{id}/waypoints calculates Haversine straight-line distance and estimated ETA with explicit estimation labels")
    @WithMockUser(username = "dispatcher@fleetx.test", roles = {"DISPATCHER"})
    void routePlanning_haversineCalculation_success() throws Exception {
        WaypointsUpdateRequest req = new WaypointsUpdateRequest(List.of(
                new WaypointItemRequest("Origin Depot Bhiwandi", 19.2968, 73.0631, 1, "ORIGIN"),
                new WaypointItemRequest("Khalapur Toll Plaza", 18.8256, 73.2891, 2, "INTERMEDIATE"),
                new WaypointItemRequest("Destination Hinjawadi Pune", 18.5913, 73.7389, 3, "DESTINATION")
        ));

        mockMvc.perform(post("/api/v1/shipments/" + testShipmentId + "/waypoints")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.estimatedDistanceKm").value(greaterThan(100.0)))
                .andExpect(jsonPath("$.data.estimatedDurationMinutes").value(greaterThan(120)))
                .andExpect(jsonPath("$.data.distanceLabel").value(containsString("Haversine")))
                .andExpect(jsonPath("$.data.etaLabel").value(containsString("Estimated Arrival Time")))
                .andExpect(jsonPath("$.data.waypoints.length()").value(3));
    }

    @Test
    @DisplayName("POST /api/v1/shipments/{id}/waypoints rejects single waypoint with 400 Bad Request")
    @WithMockUser(username = "dispatcher@fleetx.test", roles = {"DISPATCHER"})
    void routePlanning_singleWaypoint_rejected() throws Exception {
        WaypointsUpdateRequest req = new WaypointsUpdateRequest(List.of(
                new WaypointItemRequest("Only Origin", 19.2968, 73.0631, 1, "ORIGIN")
        ));

        mockMvc.perform(post("/api/v1/shipments/" + testShipmentId + "/waypoints")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_WAYPOINTS"));
    }

    @Test
    @DisplayName("POST /api/v1/shipments/{id}/waypoints rejects out-of-range coordinates with 400 Bad Request")
    @WithMockUser(username = "dispatcher@fleetx.test", roles = {"DISPATCHER"})
    void routePlanning_invalidCoordinates_rejected() throws Exception {
        WaypointsUpdateRequest req = new WaypointsUpdateRequest(List.of(
                new WaypointItemRequest("Invalid Latitude Point", 125.0, 73.0631, 1, "ORIGIN"),
                new WaypointItemRequest("Valid Destination", 18.5913, 73.7389, 2, "DESTINATION")
        ));

        mockMvc.perform(post("/api/v1/shipments/" + testShipmentId + "/waypoints")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_COORDINATES"));
    }

    @Test
    @DisplayName("Route planning whose estimated ETA exceeds promised delivery time flags shipment DELAYED and publishes alert")
    @WithMockUser(username = "dispatcher@fleetx.test", roles = {"DISPATCHER"})
    void routePlanning_etaExceedsPromisedTime_flagsDelayed() throws Exception {
        // Set promised delivery time to just 10 minutes from now, while route takes >2 hours
        Shipment s = shipmentRepository.findById(testShipmentId).orElseThrow();
        s.setPromisedDeliveryTime(Instant.now().plusSeconds(600));
        shipmentRepository.save(s);

        WaypointsUpdateRequest req = new WaypointsUpdateRequest(List.of(
                new WaypointItemRequest("Mumbai Origin", 19.0760, 72.8777, 1, "ORIGIN"),
                new WaypointItemRequest("Pune Destination", 18.5204, 73.8567, 2, "DESTINATION")
        ));

        mockMvc.perform(post("/api/v1/shipments/" + testShipmentId + "/waypoints")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());

        Shipment updated = shipmentRepository.findById(testShipmentId).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo("DELAYED");

        verify(alertPublisher).publishAlert(
                eq("SHIPMENT_DELAYED"),
                eq("WARNING"),
                eq("SHIPMENT"),
                eq(testShipmentId),
                contains("promised delivery time"),
                any()
        );
    }

    @Test
    @DisplayName("POST /api/v1/shipments/check-delayed detects overdue shipments and emits SHIPMENT_DELAYED alert")
    @WithMockUser(username = "dispatcher@fleetx.test", roles = {"DISPATCHER"})
    void checkDelayedShipments_detectsOverdue() throws Exception {
        Shipment overdue = new Shipment();
        overdue.setTrackingNumber("SHP-2026-LATE99");
        overdue.setSenderName("Late Sender");
        overdue.setSenderAddress("Origin");
        overdue.setReceiverName("Late Receiver");
        overdue.setReceiverAddress("Destination");
        overdue.setWeightKg(200.0);
        overdue.setVolumeM3(1.0);
        overdue.setStatus("IN_TRANSIT");
        overdue.setPromisedDeliveryTime(Instant.now().minusSeconds(3600)); // 1 hour ago!
        Shipment savedOverdue = shipmentRepository.save(overdue);

        mockMvc.perform(post("/api/v1/shipments/check-delayed")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.flaggedDelayedCount").value(1));

        Shipment refreshed = shipmentRepository.findById(savedOverdue.getId()).orElseThrow();
        assertThat(refreshed.getStatus()).isEqualTo("DELAYED");

        verify(alertPublisher).publishAlert(
                eq("SHIPMENT_DELAYED"),
                eq("WARNING"),
                eq("SHIPMENT"),
                eq(savedOverdue.getId()),
                contains("exceeded promised delivery time"),
                any()
        );
    }
}
