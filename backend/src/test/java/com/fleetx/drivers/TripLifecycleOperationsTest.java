package com.fleetx.drivers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fleetx.FleetxApplication;
import com.fleetx.drivers.dto.TripCreateRequest;
import com.fleetx.drivers.dto.TripStatusUpdateRequest;
import com.fleetx.drivers.entity.Driver;
import com.fleetx.drivers.entity.Trip;
import com.fleetx.drivers.entity.TripStatus;
import com.fleetx.drivers.repository.DriverPerformanceMetricsRepository;
import com.fleetx.drivers.repository.DriverRepository;
import com.fleetx.drivers.repository.TripRepository;
import com.fleetx.drivers.repository.TripStatusHistoryRepository;
import com.fleetx.fleet.entity.Vehicle;
import com.fleetx.fleet.repository.VehicleRepository;
import com.fleetx.shared.contract.AlertPublisher;
import com.fleetx.shared.contract.TripLookupService;
import com.fleetx.shared.dto.TripSummaryDto;
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

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = FleetxApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class TripLifecycleOperationsTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TripRepository tripRepository;

    @Autowired
    private TripStatusHistoryRepository historyRepository;

    @Autowired
    private DriverRepository driverRepository;

    @Autowired
    private VehicleRepository vehicleRepository;

    @Autowired
    private DriverPerformanceMetricsRepository metricsRepository;

    @Autowired
    private TripLookupService tripLookupService;

    @SpyBean
    private AlertPublisher alertPublisher;

    private Long testVehicleId;
    private Long testDriverId;

    @BeforeEach
    void setupData() {
        historyRepository.deleteAll();
        tripRepository.deleteAll();
        metricsRepository.deleteAll();
        driverRepository.deleteAll();
        vehicleRepository.deleteAll();

        // Seed available vehicle
        Vehicle vehicle = new Vehicle();
        vehicle.setVin("1HGCR2F83HA001111");
        vehicle.setLicensePlate("MH-12-TRIP-01");
        vehicle.setMake("Tata");
        vehicle.setModel("Prima 4028");
        vehicle.setModelYear(2024);
        vehicle.setVehicleType("TRUCK");
        vehicle.setFuelType("DIESEL");
        vehicle.setCapacityKg(25000.0);
        vehicle.setCapacityVolumeM3(45.0);
        vehicle.setOdometerKm(10000.0);
        vehicle.setStatus("AVAILABLE");
        vehicle.setActive(true);
        Vehicle savedV = vehicleRepository.save(vehicle);
        testVehicleId = savedV.getId();

        // Seed available driver
        Driver driver = new Driver();
        driver.setFullName("Sanjay Verma");
        driver.setPhoneNumber("+91 98765 12345");
        driver.setLicenseNumber("DL-142024-SANJAY");
        driver.setLicenseCategory("HEAVY");
        driver.setLicenseExpiryDate(LocalDate.now().plusYears(3));
        driver.setAvailabilityStatus("AVAILABLE");
        driver.setActive(true);
        Driver savedD = driverRepository.save(driver);
        testDriverId = savedD.getId();
    }

    @Test
    @DisplayName("POST /api/v1/trips successfully allocates assets and creates trip in ASSIGNED status")
    @WithMockUser(username = "dispatcher@fleetx.test", roles = {"DISPATCHER"})
    void createTrip_success() throws Exception {
        TripCreateRequest req = new TripCreateRequest(
                testVehicleId,
                testDriverId,
                "Mumbai Central Depot",
                "Pune Hinjawadi Hub",
                Instant.now().plusSeconds(3600),
                Instant.now().plusSeconds(25200),
                new BigDecimal("150.00"),
                "High priority pharmaceutical consignment"
        );

        mockMvc.perform(post("/api/v1/trips")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("ASSIGNED"))
                .andExpect(jsonPath("$.data.vehicleLicensePlate").value("MH-12-TRIP-01"))
                .andExpect(jsonPath("$.data.driverName").value("Sanjay Verma"));

        // Verify vehicle marked IN_TRIP
        Vehicle v = vehicleRepository.findById(testVehicleId).orElseThrow();
        assertThat(v.getStatus()).isEqualTo("IN_TRIP");

        // Verify driver marked ON_DUTY
        Driver d = driverRepository.findById(testDriverId).orElseThrow();
        assertThat(d.getAvailabilityStatus()).isEqualTo("ON_DUTY");

        // Verify alert emitted
        verify(alertPublisher).publishAlert(
                eq("TRIP_ASSIGNED"),
                eq("INFO"),
                eq("TRIP"),
                anyLong(),
                contains("assigned"),
                any()
        );
    }

    @Test
    @DisplayName("POST /api/v1/trips rejects assignment when vehicle is not available")
    @WithMockUser(username = "dispatcher@fleetx.test", roles = {"DISPATCHER"})
    void createTrip_vehicleUnavailable_rejected() throws Exception {
        // Set vehicle to MAINTENANCE
        Vehicle v = vehicleRepository.findById(testVehicleId).orElseThrow();
        v.setStatus("MAINTENANCE");
        vehicleRepository.save(v);

        TripCreateRequest req = new TripCreateRequest(
                testVehicleId,
                testDriverId,
                "Mumbai Central Depot",
                "Pune Hinjawadi Hub",
                Instant.now().plusSeconds(3600),
                Instant.now().plusSeconds(25200),
                new BigDecimal("150.00"),
                null
        );

        mockMvc.perform(post("/api/v1/trips")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VEHICLE_NOT_AVAILABLE"));
    }

    @Test
    @DisplayName("POST /api/v1/trips rejects assignment when driver is suspended or not eligible")
    @WithMockUser(username = "dispatcher@fleetx.test", roles = {"DISPATCHER"})
    void createTrip_driverIneligible_rejected() throws Exception {
        // Suspend driver
        Driver d = driverRepository.findById(testDriverId).orElseThrow();
        d.setAvailabilityStatus("SUSPENDED");
        driverRepository.save(d);

        TripCreateRequest req = new TripCreateRequest(
                testVehicleId,
                testDriverId,
                "Mumbai Central Depot",
                "Pune Hinjawadi Hub",
                Instant.now().plusSeconds(3600),
                Instant.now().plusSeconds(25200),
                new BigDecimal("150.00"),
                null
        );

        mockMvc.perform(post("/api/v1/trips")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("DRIVER_NOT_ELIGIBLE"));
    }

    @Test
    @DisplayName("Trip lifecycle progresses: ASSIGNED -> IN_PROGRESS -> COMPLETED and restores resources")
    @WithMockUser(username = "dispatcher@fleetx.test", roles = {"DISPATCHER"})
    void tripLifecycle_progression_success() throws Exception {
        Trip trip = new Trip();
        trip.setTripCode("TRIP-2026-9001");
        trip.setVehicleId(testVehicleId);
        trip.setDriverId(testDriverId);
        trip.setOriginAddress("Mumbai Hub");
        trip.setDestinationAddress("Pune Hub");
        trip.setStatus("ASSIGNED");
        trip.setScheduledStartTime(Instant.now());
        trip.setScheduledEndTime(Instant.now().plusSeconds(7200));
        trip.setDistanceKm(new BigDecimal("150.00"));
        Trip savedTrip = tripRepository.save(trip);

        // 1. Transition to IN_PROGRESS
        TripStatusUpdateRequest startReq = new TripStatusUpdateRequest("IN_PROGRESS", "Departed origin depot.");
        mockMvc.perform(put("/api/v1/trips/" + savedTrip.getId() + "/status")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(startReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.data.actualStartTime").isNotEmpty());

        assertThat(tripLookupService.isTripActive(savedTrip.getId())).isTrue();

        // 2. Transition to COMPLETED
        TripStatusUpdateRequest completeReq = new TripStatusUpdateRequest("COMPLETED", "Safely delivered at warehouse.");
        mockMvc.perform(put("/api/v1/trips/" + savedTrip.getId() + "/status")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(completeReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("COMPLETED"))
                .andExpect(jsonPath("$.data.actualEndTime").isNotEmpty());

        assertThat(tripLookupService.isTripActive(savedTrip.getId())).isFalse();

        // Verify assets restored to AVAILABLE
        Vehicle v = vehicleRepository.findById(testVehicleId).orElseThrow();
        assertThat(v.getStatus()).isEqualTo("AVAILABLE");

        Driver d = driverRepository.findById(testDriverId).orElseThrow();
        assertThat(d.getAvailabilityStatus()).isEqualTo("AVAILABLE");
    }

    @Test
    @DisplayName("Prevent completing a trip that has not started yet with 400 Bad Request")
    @WithMockUser(username = "dispatcher@fleetx.test", roles = {"DISPATCHER"})
    void tripLifecycle_completeWithoutStart_rejected() throws Exception {
        Trip trip = new Trip();
        trip.setTripCode("TRIP-2026-9002");
        trip.setVehicleId(testVehicleId);
        trip.setDriverId(testDriverId);
        trip.setOriginAddress("Mumbai Hub");
        trip.setDestinationAddress("Pune Hub");
        trip.setStatus("ASSIGNED"); // Still ASSIGNED, not started!
        trip.setScheduledStartTime(Instant.now());
        Trip savedTrip = tripRepository.save(trip);

        TripStatusUpdateRequest completeReq = new TripStatusUpdateRequest("COMPLETED", "Direct completion attempt");
        mockMvc.perform(put("/api/v1/trips/" + savedTrip.getId() + "/status")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(completeReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_STATE_TRANSITION"));
    }

    @Test
    @DisplayName("Prevent modifying status of already completed trip with 400 Bad Request")
    @WithMockUser(username = "dispatcher@fleetx.test", roles = {"DISPATCHER"})
    void tripLifecycle_modifyCompletedTrip_rejected() throws Exception {
        Trip trip = new Trip();
        trip.setTripCode("TRIP-2026-9003");
        trip.setVehicleId(testVehicleId);
        trip.setDriverId(testDriverId);
        trip.setOriginAddress("Mumbai Hub");
        trip.setDestinationAddress("Pune Hub");
        trip.setStatus("COMPLETED");
        trip.setScheduledStartTime(Instant.now());
        trip.setActualStartTime(Instant.now());
        trip.setActualEndTime(Instant.now().plusSeconds(3600));
        Trip savedTrip = tripRepository.save(trip);

        TripStatusUpdateRequest restartReq = new TripStatusUpdateRequest("IN_PROGRESS", "Restart attempt");
        mockMvc.perform(put("/api/v1/trips/" + savedTrip.getId() + "/status")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(restartReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_STATE_TRANSITION"));
    }

    @Test
    @DisplayName("TripLookupService contract retrieves immutable trip summary")
    void tripLookupService_contractImplementation() {
        Trip trip = new Trip();
        trip.setTripCode("TRIP-2026-9004");
        trip.setVehicleId(testVehicleId);
        trip.setDriverId(testDriverId);
        trip.setOriginAddress("Depot A");
        trip.setDestinationAddress("Hub B");
        trip.setStatus("IN_PROGRESS");
        trip.setScheduledStartTime(Instant.now());
        Trip saved = tripRepository.save(trip);

        Optional<TripSummaryDto> summary = tripLookupService.findTripSummaryById(saved.getId());
        assertThat(summary).isPresent();
        assertThat(summary.get().tripCode()).isEqualTo("TRIP-2026-9004");
        assertThat(summary.get().originAddress()).isEqualTo("Depot A");
        assertThat(summary.get().destinationAddress()).isEqualTo("Hub B");
        assertThat(tripLookupService.isTripActive(saved.getId())).isTrue();
    }
}
