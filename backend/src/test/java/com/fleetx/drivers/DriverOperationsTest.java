package com.fleetx.drivers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fleetx.FleetxApplication;
import com.fleetx.drivers.dto.DriverCreateRequest;
import com.fleetx.drivers.dto.DriverStatusUpdateRequest;
import com.fleetx.drivers.dto.DriverUpdateRequest;
import com.fleetx.drivers.entity.Driver;
import com.fleetx.drivers.repository.DriverPerformanceMetricsRepository;
import com.fleetx.drivers.repository.DriverRepository;
import com.fleetx.drivers.repository.TripRepository;
import com.fleetx.drivers.repository.TripStatusHistoryRepository;
import com.fleetx.shared.contract.AlertPublisher;
import com.fleetx.shared.contract.DriverLookupService;
import com.fleetx.shared.dto.DriverSummaryDto;
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
public class DriverOperationsTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private DriverRepository driverRepository;

    @Autowired
    private TripRepository tripRepository;

    @Autowired
    private TripStatusHistoryRepository historyRepository;

    @Autowired
    private DriverPerformanceMetricsRepository metricsRepository;

    @Autowired
    private DriverLookupService driverLookupService;

    @SpyBean
    private AlertPublisher alertPublisher;

    @BeforeEach
    void setupCleanState() {
        historyRepository.deleteAll();
        tripRepository.deleteAll();
        metricsRepository.deleteAll();
        driverRepository.deleteAll();
    }

    @Test
    @DisplayName("POST /api/v1/trips/drivers creates driver and initializes performance metrics")
    @WithMockUser(username = "dispatcher@fleetx.test", roles = {"DISPATCHER"})
    void createDriver_success() throws Exception {
        DriverCreateRequest req = new DriverCreateRequest(
                null,
                "Rajesh Sharma",
                "+91 98765 11111",
                "rajesh.sharma@fleetx.test",
                "MH-14-2024-001234",
                "HEAVY",
                LocalDate.now().plusYears(3),
                LocalDate.now().plusYears(1),
                7,
                "AVAILABLE",
                "Experienced in express highway transport"
        );

        mockMvc.perform(post("/api/v1/trips/drivers")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.fullName").value("Rajesh Sharma"))
                .andExpect(jsonPath("$.data.licenseNumber").value("MH-14-2024-001234"))
                .andExpect(jsonPath("$.data.availabilityStatus").value("AVAILABLE"))
                .andExpect(jsonPath("$.data.safetyScore").value(100.00));

        assertThat(driverRepository.findByLicenseNumber("MH-14-2024-001234")).isPresent();
    }

    @Test
    @DisplayName("POST /api/v1/trips/drivers rejects duplicate license number with 409 Conflict")
    @WithMockUser(username = "dispatcher@fleetx.test", roles = {"DISPATCHER"})
    void createDriver_duplicateLicense_returns409() throws Exception {
        Driver driver = new Driver();
        driver.setFullName("Existing Driver");
        driver.setPhoneNumber("+91 98765 22222");
        driver.setLicenseNumber("MH-14-DUPLICATE-01");
        driver.setLicenseExpiryDate(LocalDate.now().plusYears(2));
        driverRepository.save(driver);

        DriverCreateRequest req = new DriverCreateRequest(
                null,
                "Duplicate Applicant",
                "+91 98765 33333",
                "dup@fleetx.test",
                "MH-14-DUPLICATE-01",
                "HEAVY",
                LocalDate.now().plusYears(1),
                null,
                2,
                "AVAILABLE",
                null
        );

        mockMvc.perform(post("/api/v1/trips/drivers")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("RESOURCE_CONFLICT"));
    }

    @Test
    @DisplayName("POST /api/v1/trips/drivers rejects past license expiry date with 400 Bad Request")
    @WithMockUser(username = "dispatcher@fleetx.test", roles = {"DISPATCHER"})
    void createDriver_pastLicenseExpiry_rejected() throws Exception {
        DriverCreateRequest req = new DriverCreateRequest(
                null,
                "Expired Applicant",
                "+91 98765 44444",
                "expired@fleetx.test",
                "MH-14-EXPIRED-99",
                "MEDIUM",
                LocalDate.now().minusDays(10), // in the past!
                null,
                1,
                "AVAILABLE",
                null
        );

        mockMvc.perform(post("/api/v1/trips/drivers")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"));
    }

    @Test
    @DisplayName("GET /api/v1/trips/drivers retrieves filtered and paginated drivers")
    @WithMockUser(username = "viewer@fleetx.test", roles = {"VIEWER"})
    void getDrivers_filtersAndPagination() throws Exception {
        Driver d1 = new Driver();
        d1.setFullName("Anil Deshmukh");
        d1.setPhoneNumber("+91 98765 00001");
        d1.setLicenseNumber("MH-01-ANIL-100");
        d1.setLicenseExpiryDate(LocalDate.now().plusYears(1));
        d1.setAvailabilityStatus("AVAILABLE");
        driverRepository.save(d1);

        Driver d2 = new Driver();
        d2.setFullName("Sunil Shinde");
        d2.setPhoneNumber("+91 98765 00002");
        d2.setLicenseNumber("MH-02-SUNIL-200");
        d2.setLicenseExpiryDate(LocalDate.now().plusYears(1));
        d2.setAvailabilityStatus("ON_DUTY");
        driverRepository.save(d2);

        mockMvc.perform(get("/api/v1/trips/drivers")
                        .param("availability", "AVAILABLE")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.content[0].fullName").value("Anil Deshmukh"));
    }

    @Test
    @DisplayName("DriverLookupService contract methods function correctly across module boundaries")
    void driverLookupService_contractImplementation() {
        Driver driver = new Driver();
        driver.setFullName("Mahesh Patil");
        driver.setPhoneNumber("+91 98765 77777");
        driver.setLicenseNumber("MH-12-MAHESH-77");
        driver.setLicenseCategory("HEAVY");
        driver.setLicenseExpiryDate(LocalDate.now().plusYears(2));
        driver.setAvailabilityStatus("AVAILABLE");
        Driver saved = driverRepository.save(driver);

        // findDriverSummaryById
        Optional<DriverSummaryDto> summary = driverLookupService.findDriverSummaryById(saved.getId());
        assertThat(summary).isPresent();
        assertThat(summary.get().licenseNumber()).isEqualTo("MH-12-MAHESH-77");
        assertThat(summary.get().availabilityStatus()).isEqualTo("AVAILABLE");

        // isDriverEligibleForTrip
        assertThat(driverLookupService.isDriverEligibleForTrip(saved.getId())).isTrue();

        // setDriverAvailability
        driverLookupService.setDriverAvailability(saved.getId(), "ON_DUTY");
        assertThat(driverLookupService.isDriverEligibleForTrip(saved.getId())).isFalse();
    }

    @Test
    @DisplayName("DriverLookupService marks driver with expired license as ineligible for trips")
    void driverLookupService_expiredLicense_notEligible() {
        Driver driver = new Driver();
        driver.setFullName("Expired Driver");
        driver.setPhoneNumber("+91 98765 88888");
        driver.setLicenseNumber("MH-12-EXP-88");
        driver.setLicenseExpiryDate(LocalDate.now().minusDays(5));
        driver.setAvailabilityStatus("AVAILABLE");
        Driver saved = driverRepository.save(driver);

        assertThat(driverLookupService.isDriverEligibleForTrip(saved.getId())).isFalse();
    }

    @Test
    @DisplayName("POST /api/v1/trips/drivers publishes alert when driver license expires within 30 days")
    @WithMockUser(username = "dispatcher@fleetx.test", roles = {"DISPATCHER"})
    void createDriver_expiringSoon_publishesAlert() throws Exception {
        DriverCreateRequest req = new DriverCreateRequest(
                null,
                "Vikas Jagtap",
                "+91 98765 99999",
                "vikas@fleetx.test",
                "MH-14-VIKAS-99",
                "HEAVY",
                LocalDate.now().plusDays(15), // Expiring in 15 days (<30 days threshold)!
                null,
                5,
                "AVAILABLE",
                null
        );

        mockMvc.perform(post("/api/v1/trips/drivers")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated());

        verify(alertPublisher).publishAlert(
                eq("DRIVER_LICENSE_EXPIRING"),
                eq("WARNING"),
                eq("DRIVER"),
                anyLong(),
                contains("expires on"),
                any()
        );
    }
}
