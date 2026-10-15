package com.fleetx.tracking;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fleetx.tracking.dto.*;
import com.fleetx.tracking.service.GeofenceAndTelemetryMathService;
import com.fleetx.tracking.service.TrackingService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class SimulationAndGeofenceTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private GeofenceAndTelemetryMathService mathService;

    @Autowired
    private TrackingService trackingService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Long insertEligibleTestVehicle() {
        String suffix = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        String vin = "1HGCM82633A" + suffix;
        String plate = "MH-01-" + suffix;
        jdbcTemplate.update("""
                INSERT INTO vehicles (vin, license_plate, make, model, model_year, vehicle_type, fuel_type,
                                      capacity_kg, capacity_volume_m3, odometer_km, status, active, created_at)
                VALUES (?, ?, 'Tata', 'Prima 5530', 2025, 'TRUCK', 'DIESEL', 28000.0, 55.0, 15000.0, 'AVAILABLE', TRUE, CURRENT_TIMESTAMP)
                """, vin, plate);
        return jdbcTemplate.queryForObject(
                "SELECT id FROM vehicles WHERE license_plate = ?",
                Long.class,
                plate
        );
    }

    @Test
    @DisplayName("1. Waypoint interpolation and stationary / short-interval speed & heading math work safely")
    void interpolationAndSafeMathWorkCorrectly() {
        SimulatorWaypointDto wpStart = new SimulatorWaypointDto(19.0000, 72.8000, "Origin");
        SimulatorWaypointDto wpEnd = new SimulatorWaypointDto(18.5000, 73.8000, "Destination");

        SimulatorWaypointDto midpoint = mathService.interpolatePoint(wpStart, wpEnd, 0.5);
        assertThat(midpoint.latitude()).isEqualTo(18.75);
        assertThat(midpoint.longitude()).isEqualTo(73.3);

        // Stationary vehicle (identical coordinates) -> speed = 0.0, heading = fallbackHeading without NaN
        double stationaryBearing = mathService.calculateBearingDegrees(19.0, 72.8, 19.0, 72.8, 135.5);
        assertThat(stationaryBearing).isEqualTo(135.5);
        assertThat(mathService.calculateSafeSpeedKmh(0.0, 2.0)).isEqualTo(0.0);

        // Very short / zero time interval -> safe 0.0 instead of Infinity
        assertThat(mathService.calculateSafeSpeedKmh(0.5, 0.0)).isEqualTo(0.0);
        assertThat(mathService.calculateSafeSpeedKmh(0.5, 0.01)).isEqualTo(0.0);
    }

    @Test
    @WithMockUser(username = "dispatcher@fleetx.com", roles = {"DISPATCHER"})
    @DisplayName("2. Rejects out-of-range coordinates and negative speed on telemetry ingestion")
    void rejectsInvalidCoordinatesAndNegativeSpeed() throws Exception {
        Long vehicleId = insertEligibleTestVehicle();

        TelemetryPingDto invalidLatPing = new TelemetryPingDto(
                vehicleId, null, 98.5, 72.8777, 50.0, 90.0, 15010.0, Instant.now()
        );

        mockMvc.perform(post("/api/v1/tracking/telemetry")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidLatPing)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_LATITUDE"));

        assertThatThrownBy(() -> mathService.validateTelemetryInputs(19.0, 72.8, -15.0, Instant.now()))
                .hasMessageContaining("Speed (km/h) must be between 0.0");
    }

    @Test
    @WithMockUser(username = "dispatcher@fleetx.com", roles = {"DISPATCHER"})
    @DisplayName("3. Simulator lifecycle (start, multiple steps, pause, resume, stop) manages scheduled tasks cleanly")
    void simulatorLifecycleAndTaskCleanupWork() throws Exception {
        Long vehicleId = insertEligibleTestVehicle();

        SimulatorStartRequest startReq = new SimulatorStartRequest(
                vehicleId,
                null,
                2,
                2.0,
                65.0,
                List.of(
                        new SimulatorWaypointDto(19.0760, 72.8777, "Mumbai Hub"),
                        new SimulatorWaypointDto(18.7546, 73.4062, "Lonavala Hub"),
                        new SimulatorWaypointDto(18.5204, 73.8567, "Pune Hub")
                )
        );

        // Start simulator twice to ensure no unbounded duplicate tasks are created
        trackingService.startSimulation(startReq);
        SimulatorSessionDto started = trackingService.startSimulation(startReq);

        assertThat(started.simulatorStatus()).isEqualTo("RUNNING");
        assertThat(started.simulated()).isTrue();
        assertThat(started.dataSource()).isEqualTo("SIMULATED_GPS_TELEMETRY");
        assertThat(trackingService.isTaskScheduledForVehicle(vehicleId)).isTrue();

        double initialLat = started.latitude();
        double initialLon = started.longitude();

        // Advance two deterministic steps
        SimulatorSessionDto step1 = trackingService.stepSimulation(vehicleId);
        SimulatorSessionDto step2 = trackingService.stepSimulation(vehicleId);

        assertThat(step2.latitude()).isNotEqualTo(initialLat);
        assertThat(step2.longitude()).isNotEqualTo(initialLon);
        assertThat(step2.currentOdometerKm()).isGreaterThan(started.currentOdometerKm());
        assertThat(step2.segmentProgress()).isGreaterThan(step1.segmentProgress());

        // Pause simulator -> scheduled task must stop immediately
        SimulatorSessionDto paused = trackingService.pauseSimulation(vehicleId);
        assertThat(paused.simulatorStatus()).isEqualTo("PAUSED");
        assertThat(trackingService.isTaskScheduledForVehicle(vehicleId)).isFalse();

        // Resume simulator -> scheduled task resumes from paused position
        SimulatorSessionDto resumed = trackingService.resumeSimulation(vehicleId);
        assertThat(resumed.simulatorStatus()).isEqualTo("RUNNING");
        assertThat(resumed.latitude()).isEqualTo(paused.latitude());
        assertThat(trackingService.isTaskScheduledForVehicle(vehicleId)).isTrue();

        // Stop simulator -> scheduled task is cancelled and removed
        mockMvc.perform(post("/api/v1/tracking/simulator/" + vehicleId + "/stop")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.simulatorStatus").value("STOPPED"));

        assertThat(trackingService.isTaskScheduledForVehicle(vehicleId)).isFalse();

        // Verify persisted telemetry history exists
        List<TelemetryPingDto> history = trackingService.getVehicleTrackingHistory(vehicleId, 10);
        assertThat(history).hasSizeGreaterThanOrEqualTo(3);
    }

    @Test
    @WithMockUser(username = "admin@fleetx.com", roles = {"ADMIN"})
    @DisplayName("4. Circular geofence detects exit once and suppresses duplicate alerts on subsequent outside ticks")
    void circularGeofenceExitSuppressesRepeatedOutsideTicks() throws Exception {
        Long vehicleId = insertEligibleTestVehicle();

        // Create a 500m circular depot geofence around (19.0760, 72.8777)
        GeofenceCreateRequest fenceReq = new GeofenceCreateRequest(
                "Mumbai Depot Circle " + UUID.randomUUID().toString().substring(0, 5),
                19.0760,
                72.8777,
                500.0,
                "DEPOT",
                "CIRCLE",
                null,
                40.0
        );
        GeofenceResponseDto createdFence = trackingService.createGeofence(fenceReq);
        assertThat(createdFence.id()).isNotNull();

        // Tick 1: Inside depot -> records initial INSIDE state, no exit alert
        trackingService.ingestSimulatedTelemetry(new TelemetryPingDto(
                vehicleId, null, 19.0760, 72.8777, 25.0, 90.0, 15000.0, Instant.now()
        ));

        Long exitAlertCountAfterTick1 = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM alerts WHERE entity_type = 'VEHICLE' AND entity_id = ? AND alert_type = 'GEOFENCE_EXIT'",
                Long.class, vehicleId
        );
        assertThat(exitAlertCountAfterTick1).isEqualTo(0L);

        // Tick 2: Vehicle moves outside the 500m depot boundary -> triggers GEOFENCE_EXIT alert #1
        trackingService.ingestSimulatedTelemetry(new TelemetryPingDto(
                vehicleId, null, 19.0950, 72.9000, 55.0, 90.0, 15003.0, Instant.now()
        ));

        Long exitAlertCountAfterTick2 = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM alerts WHERE entity_type = 'VEHICLE' AND entity_id = ? AND alert_type = 'GEOFENCE_EXIT'",
                Long.class, vehicleId
        );
        assertThat(exitAlertCountAfterTick2).isEqualTo(1L);

        // Even if operator acknowledges the alert while vehicle is still outside,
        // subsequent ticks outside must NOT re-trigger GEOFENCE_EXIT!
        Long alertId = jdbcTemplate.queryForObject(
                "SELECT id FROM alerts WHERE entity_type = 'VEHICLE' AND entity_id = ? AND alert_type = 'GEOFENCE_EXIT'",
                Long.class, vehicleId
        );
        trackingService.acknowledgeAlert(alertId, "admin@fleetx.com");

        // Tick 3 & Tick 4: Vehicle remains outside boundary -> 0 new exit alerts!
        trackingService.ingestSimulatedTelemetry(new TelemetryPingDto(
                vehicleId, null, 19.1100, 72.9200, 58.0, 90.0, 15006.0, Instant.now()
        ));
        trackingService.ingestSimulatedTelemetry(new TelemetryPingDto(
                vehicleId, null, 19.1250, 72.9400, 60.0, 90.0, 15009.0, Instant.now()
        ));

        Long exitAlertCountAfterTick4 = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM alerts WHERE entity_type = 'VEHICLE' AND entity_id = ? AND alert_type = 'GEOFENCE_EXIT'",
                Long.class, vehicleId
        );
        assertThat(exitAlertCountAfterTick4).isEqualTo(1L);
    }

    @Test
    @WithMockUser(username = "admin@fleetx.com", roles = {"ADMIN"})
    @DisplayName("5. Polygonal geofence accurately evaluates point-in-polygon entry and exit")
    void polygonalGeofenceBoundaryEvaluationWorks() {
        List<List<Double>> squarePolygon = List.of(
                List.of(18.50, 73.80),
                List.of(18.50, 73.90),
                List.of(18.60, 73.90),
                List.of(18.60, 73.80)
        );

        assertThat(mathService.isInsidePolygon(18.55, 73.85, squarePolygon)).isTrue();
        assertThat(mathService.isInsidePolygon(18.70, 73.85, squarePolygon)).isFalse();

        GeofenceCreateRequest polyReq = new GeofenceCreateRequest(
                "Pune Polygon Zone " + UUID.randomUUID().toString().substring(0, 5),
                18.55,
                73.85,
                5000.0,
                "RESTRICTED",
                "POLYGON",
                squarePolygon,
                30.0
        );
        GeofenceResponseDto savedPoly = trackingService.createGeofence(polyReq);
        assertThat(savedPoly.shapeType()).isEqualTo("POLYGON");
        assertThat(savedPoly.polygonCoordinates()).hasSize(4);
    }
}
