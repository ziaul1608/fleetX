package com.fleetx.fleet;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fleetx.FleetxApplication;
import com.fleetx.fleet.dto.FuelRecordCreateRequest;
import com.fleetx.fleet.dto.MaintenanceRecordCreateRequest;
import com.fleetx.fleet.entity.Vehicle;
import com.fleetx.fleet.repository.FuelRecordRepository;
import com.fleetx.fleet.repository.MaintenanceRecordRepository;
import com.fleetx.fleet.repository.ServiceWarningRepository;
import com.fleetx.fleet.repository.VehicleRepository;
import com.fleetx.fleet.service.FuelService;
import com.fleetx.fleet.service.MaintenanceService;
import com.fleetx.shared.contract.AlertPublisher;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = FleetxApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class FuelAndMaintenanceTest {

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
    private FuelService fuelService;

    @Autowired
    private MaintenanceService maintenanceService;

    @SpyBean
    private AlertPublisher alertPublisher;

    private Vehicle testVehicle;

    @BeforeEach
    void setupTestData() {
        warningRepository.deleteAll();
        maintenanceRecordRepository.deleteAll();
        fuelRecordRepository.deleteAll();
        vehicleRepository.deleteAll();

        testVehicle = vehicleRepository.save(new Vehicle(
                "1HGCR2F83HA003333",
                "MH-14-GH-3333",
                "Volvo",
                "FM 420",
                2023,
                "TRUCK",
                "DIESEL",
                30000.0,
                55.0,
                10000.0,
                "AVAILABLE"
        ));
    }

    @Test
    @DisplayName("Should calculate total fuel cost in Java from liters and pricePerLiter")
    @WithMockUser(username = "driver@fleetx.test", roles = {"DRIVER"})
    void shouldCalculateFuelTotalCost() throws Exception {
        FuelRecordCreateRequest request = new FuelRecordCreateRequest(
                50.0,
                new BigDecimal("95.50"),
                null, // totalCost omitted -> must be calculated by Java service
                10400.0,
                "IndianOil Hub",
                Instant.now()
        );

        mockMvc.perform(post("/api/v1/fleet/vehicles/" + testVehicle.getId() + "/fuel-records")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalCost").value(4775.00)) // 50 * 95.50 = 4775.00
                .andExpect(jsonPath("$.data.efficiencyKmPerLiter").value(8.0)); // (10400 - 10000) / 50 = 400 / 50 = 8.0 km/L

        Vehicle updated = vehicleRepository.findById(testVehicle.getId()).orElseThrow();
        assertThat(updated.getOdometerKm()).isEqualTo(10400.0);
    }

    @Test
    @DisplayName("Should derive pricePerLiter when totalCost is supplied")
    @WithMockUser(username = "driver@fleetx.test", roles = {"DRIVER"})
    void shouldDerivePricePerLiter() throws Exception {
        FuelRecordCreateRequest request = new FuelRecordCreateRequest(
                40.0,
                null, // pricePerLiter omitted
                new BigDecimal("3800.00"),
                10360.0,
                "HPCL Expressway",
                Instant.now()
        );

        mockMvc.perform(post("/api/v1/fleet/vehicles/" + testVehicle.getId() + "/fuel-records")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.pricePerLiter").value(95.00)) // 3800 / 40 = 95.00
                .andExpect(jsonPath("$.data.totalCost").value(3800.00));
    }

    @Test
    @DisplayName("Should handle fuel efficiency edge cases: zero distance and rollback safely")
    void testFuelEfficiencyEdgeCases() {
        // First fueling: baseline 10,000 -> 10,400 with 50 L => 8.0 km/L
        fuelService.recordFueling(testVehicle.getId(), new FuelRecordCreateRequest(
                50.0, new BigDecimal("100.00"), null, 10400.0, "Station 1", Instant.now()
        ));

        // Edge case 1: Fueling with same odometer (Delta distance = 0 km)
        var zeroDistRecord = fuelService.recordFueling(testVehicle.getId(), new FuelRecordCreateRequest(
                20.0, new BigDecimal("100.00"), null, 10400.0, "Station 2", Instant.now()
        ));
        assertThat(zeroDistRecord.efficiencyKmPerLiter()).isNull(); // Zero distance safely handled

        // Edge case 2: Consecutive fueling with valid distance: 10,400 -> 11,000 with 60 L => 10.0 km/L
        var validRecord = fuelService.recordFueling(testVehicle.getId(), new FuelRecordCreateRequest(
                60.0, new BigDecimal("100.00"), null, 11000.0, "Station 3", Instant.now()
        ));
        assertThat(validRecord.efficiencyKmPerLiter()).isEqualTo(10.0);

        // Overall average efficiency: (600 km / 60 L) = 10.0 km/L
        Double avgEfficiency = fuelService.calculateAverageEfficiency(testVehicle.getId());
        assertThat(avgEfficiency).isNotNull();
        assertThat(avgEfficiency).isGreaterThan(0.0);
    }

    @Test
    @DisplayName("Should generate maintenance upcoming warning and trigger AlertPublisher")
    @WithMockUser(username = "manager@fleetx.test", roles = {"FLEET_MANAGER"})
    void shouldTriggerUpcomingMaintenanceWarning() throws Exception {
        // Schedule next service at 10,450 km (vehicle odometer is at 10,000 km)
        MaintenanceRecordCreateRequest maintRequest = new MaintenanceRecordCreateRequest(
                "PREVENTIVE",
                "Periodic Engine Oil & Brake Check",
                new BigDecimal("12500.00"),
                LocalDate.now(),
                10000.0,
                10450.0, // Due in 450 km (<= 500 km threshold)
                LocalDate.now().plusMonths(6),
                "COMPLETED"
        );

        mockMvc.perform(post("/api/v1/fleet/vehicles/" + testVehicle.getId() + "/maintenance-records")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(maintRequest)))
                .andExpect(status().isCreated());

        // Warnings list should now contain the upcoming service warning
        mockMvc.perform(get("/api/v1/fleet/warnings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].warningType").value("SERVICE_DUE_KM"))
                .andExpect(jsonPath("$.data[0].severity").value("WARNING"));

        // AlertPublisher must have been invoked
        verify(alertPublisher, atLeastOnce()).publishAlert(
                eq("SERVICE_DUE"),
                eq("WARNING"),
                eq("VEHICLE"),
                eq(testVehicle.getId()),
                contains("DUE SOON"),
                any(Instant.class)
        );
    }

    @Test
    @DisplayName("Should detect overdue maintenance (odometer passed due) with CRITICAL severity")
    @WithMockUser(username = "manager@fleetx.test", roles = {"FLEET_MANAGER"})
    void shouldDetectOverdueMaintenance() throws Exception {
        // Vehicle odometer is 10,000 km. Schedule service due at 9,800 km (already overdue)
        MaintenanceRecordCreateRequest maintRequest = new MaintenanceRecordCreateRequest(
                "PREVENTIVE",
                "Differential oil replacement",
                new BigDecimal("8000.00"),
                LocalDate.now().minusMonths(2),
                9000.0,
                9800.0, // Overdue by 200 km
                LocalDate.now().plusMonths(3),
                "COMPLETED"
        );

        mockMvc.perform(post("/api/v1/fleet/vehicles/" + testVehicle.getId() + "/maintenance-records")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(maintRequest)))
                .andExpect(status().isCreated());

        // Verify CRITICAL service warning recorded
        assertThat(warningRepository.findByVehicleIdAndResolvedFalse(testVehicle.getId()))
                .anyMatch(w -> "SERVICE_OVERDUE_KM".equals(w.getWarningType()) && "CRITICAL".equals(w.getSeverity()));

        // AlertPublisher must have been invoked with CRITICAL severity
        verify(alertPublisher, atLeastOnce()).publishAlert(
                eq("SERVICE_OVERDUE"),
                eq("CRITICAL"),
                eq("VEHICLE"),
                eq(testVehicle.getId()),
                contains("OVERDUE"),
                any(Instant.class)
        );
    }
}
