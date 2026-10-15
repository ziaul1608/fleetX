package com.fleetx.fleet;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fleetx.FleetxApplication;
import com.fleetx.fleet.dto.VehicleCreateRequest;
import com.fleetx.fleet.dto.VehicleStatusUpdateRequest;
import com.fleetx.fleet.dto.VehicleUpdateRequest;
import com.fleetx.fleet.entity.Vehicle;
import com.fleetx.fleet.repository.FuelRecordRepository;
import com.fleetx.fleet.repository.MaintenanceRecordRepository;
import com.fleetx.fleet.repository.ServiceWarningRepository;
import com.fleetx.fleet.repository.VehicleRepository;
import com.fleetx.shared.contract.VehicleLookupService;
import com.fleetx.shared.dto.VehicleSummaryDto;
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

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = FleetxApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class VehicleOperationsTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private VehicleRepository vehicleRepository;

    @Autowired
    private FuelRecordRepository fuelRecordRepository;

    @Autowired
    private MaintenanceRecordRepository maintenanceRecordRepository;

    @Autowired
    private ServiceWarningRepository warningRepository;

    @Autowired
    private VehicleLookupService vehicleLookupService;

    @BeforeEach
    void setupCleanState() {
        warningRepository.deleteAll();
        maintenanceRecordRepository.deleteAll();
        fuelRecordRepository.deleteAll();
        vehicleRepository.deleteAll();
    }

    @Test
    @DisplayName("Should successfully create a new vehicle with valid data")
    @WithMockUser(username = "manager@fleetx.test", roles = {"FLEET_MANAGER"})
    void shouldCreateVehicleSuccessfully() throws Exception {
        VehicleCreateRequest request = new VehicleCreateRequest(
                "1HGCR2F83HA001234",
                "MH-12-AB-1234",
                "Tata",
                "Prima 4028",
                2024,
                "TRUCK",
                "DIESEL",
                25000.0,
                45.0,
                12500.0,
                "AVAILABLE"
        );

        mockMvc.perform(post("/api/v1/fleet/vehicles")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.vin").value("1HGCR2F83HA001234"))
                .andExpect(jsonPath("$.data.licensePlate").value("MH-12-AB-1234"))
                .andExpect(jsonPath("$.data.status").value("AVAILABLE"))
                .andExpect(jsonPath("$.data.active").value(true));

        assertThat(vehicleRepository.findByLicensePlate("MH-12-AB-1234")).isPresent();
    }

    @Test
    @DisplayName("Should reject duplicate VIN with 409 Conflict")
    @WithMockUser(username = "manager@fleetx.test", roles = {"FLEET_MANAGER"})
    void shouldRejectDuplicateVin() throws Exception {
        Vehicle existing = new Vehicle(
                "1HGCR2F83HA001234",
                "MH-12-AB-1234",
                "Tata",
                "Prima",
                2023,
                "TRUCK",
                "DIESEL",
                20000.0,
                40.0,
                5000.0,
                "AVAILABLE"
        );
        vehicleRepository.save(existing);

        VehicleCreateRequest duplicateRequest = new VehicleCreateRequest(
                "1HGCR2F83HA001234",
                "MH-12-CD-5678",
                "Volvo",
                "FH16",
                2024,
                "TRUCK",
                "DIESEL",
                28000.0,
                50.0,
                100.0,
                "AVAILABLE"
        );

        mockMvc.perform(post("/api/v1/fleet/vehicles")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicateRequest)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("RESOURCE_CONFLICT"));
    }

    @Test
    @DisplayName("Should reject duplicate license plate with 409 Conflict")
    @WithMockUser(username = "manager@fleetx.test", roles = {"FLEET_MANAGER"})
    void shouldRejectDuplicateLicensePlate() throws Exception {
        Vehicle existing = new Vehicle(
                "1HGCR2F83HA001111",
                "MH-12-AB-9999",
                "Tata",
                "Prima",
                2023,
                "TRUCK",
                "DIESEL",
                20000.0,
                40.0,
                5000.0,
                "AVAILABLE"
        );
        vehicleRepository.save(existing);

        VehicleCreateRequest duplicateRequest = new VehicleCreateRequest(
                "1HGCR2F83HA002222",
                "MH-12-AB-9999",
                "Volvo",
                "FH16",
                2024,
                "TRUCK",
                "DIESEL",
                28000.0,
                50.0,
                100.0,
                "AVAILABLE"
        );

        mockMvc.perform(post("/api/v1/fleet/vehicles")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicateRequest)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("RESOURCE_CONFLICT"));
    }

    @Test
    @DisplayName("Should fail validation on invalid VIN or negative payload capacity")
    @WithMockUser(username = "manager@fleetx.test", roles = {"FLEET_MANAGER"})
    void shouldFailValidationOnMalformedData() throws Exception {
        VehicleCreateRequest malformed = new VehicleCreateRequest(
                "INVALID-SHORT-VIN",
                "",
                "Tata",
                "Prima",
                2024,
                "TRUCK",
                "DIESEL",
                -500.0,
                40.0,
                0.0,
                "AVAILABLE"
        );

        mockMvc.perform(post("/api/v1/fleet/vehicles")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(malformed)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"));
    }

    @Test
    @DisplayName("Should reject arbitrary status string with 400 Bad Request")
    @WithMockUser(username = "manager@fleetx.test", roles = {"FLEET_MANAGER"})
    void shouldRejectArbitraryStatusString() throws Exception {
        Vehicle vehicle = vehicleRepository.save(new Vehicle(
                "1HGCR2F83HA009999",
                "MH-12-XY-9999",
                "Eicher",
                "Pro 3015",
                2022,
                "TRUCK",
                "DIESEL",
                15000.0,
                30.0,
                20000.0,
                "AVAILABLE"
        ));

        VehicleStatusUpdateRequest badStatus = new VehicleStatusUpdateRequest("FLYING_IN_SPACE", "Testing");

        mockMvc.perform(patch("/api/v1/fleet/vehicles/" + vehicle.getId() + "/status")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(badStatus)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_VEHICLE_STATUS"));
    }

    @Test
    @DisplayName("Should update vehicle details and toggle active state")
    @WithMockUser(username = "admin@fleetx.test", roles = {"ADMIN"})
    void shouldUpdateVehicleSuccessfully() throws Exception {
        Vehicle vehicle = vehicleRepository.save(new Vehicle(
                "1HGCR2F83HA008888",
                "MH-12-KL-8888",
                "Ashok Leyland",
                "Boss 1215",
                2023,
                "TRUCK",
                "DIESEL",
                12000.0,
                25.0,
                15000.0,
                "AVAILABLE"
        ));

        VehicleUpdateRequest updateRequest = new VehicleUpdateRequest(
                "Ashok Leyland",
                "Boss 1215 LE",
                2023,
                "TRUCK",
                "DIESEL",
                14000.0,
                28.0,
                "MAINTENANCE",
                false
        );

        mockMvc.perform(put("/api/v1/fleet/vehicles/" + vehicle.getId())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.model").value("Boss 1215 LE"))
                .andExpect(jsonPath("$.data.status").value("MAINTENANCE"))
                .andExpect(jsonPath("$.data.active").value(false));

        Vehicle updated = vehicleRepository.findById(vehicle.getId()).orElseThrow();
        assertThat(updated.getActive()).isFalse();
        assertThat(updated.getStatus()).isEqualTo("MAINTENANCE");
    }

    @Test
    @DisplayName("VehicleLookupService contract: should report availability accurately")
    void testVehicleLookupServiceContract() {
        Vehicle v = new Vehicle(
                "1HGCR2F83HA007777",
                "MH-12-MN-7777",
                "Mahindra",
                "Blazo X 28",
                2023,
                "TRUCK",
                "DIESEL",
                28000.0,
                48.0,
                8000.0,
                "AVAILABLE"
        );
        Vehicle saved = vehicleRepository.save(v);

        // 1. Initially available
        assertThat(vehicleLookupService.isVehicleAvailableForTrip(saved.getId())).isTrue();

        Optional<VehicleSummaryDto> summaryOpt = vehicleLookupService.findVehicleSummaryById(saved.getId());
        assertThat(summaryOpt).isPresent();
        assertThat(summaryOpt.get().licensePlate()).isEqualTo("MH-12-MN-7777");

        // 2. Change status to IN_TRIP via lookup contract
        vehicleLookupService.updateVehicleOperationalStatus(saved.getId(), "IN_TRIP");
        assertThat(vehicleLookupService.isVehicleAvailableForTrip(saved.getId())).isFalse();

        // 3. Deactivated vehicle is not available
        saved.setStatus("AVAILABLE");
        saved.setActive(false);
        vehicleRepository.save(saved);
        assertThat(vehicleLookupService.isVehicleAvailableForTrip(saved.getId())).isFalse();
    }
}
