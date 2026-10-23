package com.fleetx.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fleetx.FleetxApplication;
import com.fleetx.auth.dto.SendSignupOtpRequest;
import com.fleetx.auth.dto.VerifySignupOtpRequest;
import com.fleetx.auth.service.MockEmailService;
import com.fleetx.drivers.dto.DriverCreateRequest;
import com.fleetx.drivers.dto.TripCreateRequest;
import com.fleetx.fleet.dto.VehicleCreateRequest;
import com.fleetx.shipments.dto.ShipmentCreateRequest;
import com.fleetx.tracking.dto.GeofenceCreateRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = FleetxApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class MultiTenantIsolationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MockEmailService mockEmailService;

    @BeforeEach
    void setUp() {
        mockEmailService.clear();
    }

    private MockHttpSession registerAndLoginTenant(String email, String companyName, String warehouseName, String ownerName) throws Exception {
        mockMvc.perform(post("/api/v1/auth/otp/send-signup")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SendSignupOtpRequest(email, companyName, warehouseName, ownerName))))
                .andExpect(status().isOk());

        String otp = mockEmailService.getLastSentOtp(email).orElseThrow();

        MvcResult result = mockMvc.perform(post("/api/v1/auth/otp/verify-signup")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new VerifySignupOtpRequest(email, otp, companyName, warehouseName, ownerName))))
                .andExpect(status().isOk())
                .andReturn();

        return (MockHttpSession) result.getRequest().getSession(false);
    }

    @Test
    @DisplayName("Should strictly isolate data across all modules between independent tenants")
    void testStrictMultiTenantIsolationAcrossAllModules() throws Exception {
        // 1. Onboard Tenant Alpha
        MockHttpSession sessionA = registerAndLoginTenant(
                "owner@alpha-logistics.test",
                "Alpha Global Freight",
                "Alpha Port Depot",
                "Alice Alpha"
        );

        // 2. Onboard Tenant Beta
        MockHttpSession sessionB = registerAndLoginTenant(
                "owner@beta-express.test",
                "Beta Express Line",
                "Beta Central Hub",
                "Bob Beta"
        );

        // 3. Tenant Alpha creates assets across modules:
        // A) Vehicle
        VehicleCreateRequest vehicleReq = new VehicleCreateRequest(
                "1HGCR2F83HA001111",
                "MH-12-AL-1111",
                "Volvo",
                "FH16",
                2024,
                "TRUCK",
                "DIESEL",
                25000.0,
                55.0,
                12500.0,
                "AVAILABLE"
        );
        MvcResult vRes = mockMvc.perform(post("/api/v1/fleet/vehicles")
                        .with(csrf())
                        .session(sessionA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(vehicleReq)))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode vNode = objectMapper.readTree(vRes.getResponse().getContentAsString());
        Long vehicleAId = vNode.path("data").path("id").asLong();

        // B) Driver
        DriverCreateRequest driverReq = new DriverCreateRequest(
                null,
                "David Alpha Driver",
                "+919876543210",
                "alpha.driver@alpha.test",
                "DL-AL-998877",
                "HEAVY",
                java.time.LocalDate.now().plusYears(2),
                java.time.LocalDate.now().plusYears(1),
                5,
                "AVAILABLE",
                "Alpha primary driver"
        );
        MvcResult dRes = mockMvc.perform(post("/api/v1/trips/drivers")
                        .with(csrf())
                        .session(sessionA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(driverReq)))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode dNode = objectMapper.readTree(dRes.getResponse().getContentAsString());
        Long driverAId = dNode.path("data").path("id").asLong();

        // C) Trip
        TripCreateRequest tripReq = new TripCreateRequest(
                vehicleAId,
                driverAId,
                "Mumbai Port Gateway",
                "Pune MIDC Bhosari",
                Instant.now().plusSeconds(3600),
                Instant.now().plusSeconds(18000),
                java.math.BigDecimal.valueOf(165.5),
                "Alpha corridor trip"
        );
        MvcResult tRes = mockMvc.perform(post("/api/v1/trips")
                        .with(csrf())
                        .session(sessionA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(tripReq)))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode tNode = objectMapper.readTree(tRes.getResponse().getContentAsString());
        Long tripAId = tNode.path("data").path("id").asLong();

        // D) Shipment
        ShipmentCreateRequest shipmentReq = new ShipmentCreateRequest(
                null,
                "Atlas Industrial Corp",
                "Mumbai Hub B",
                "Apex Electronics",
                "Pune Sector 4",
                "Precision tooling parts",
                420.0,
                3.5,
                Instant.now().plusSeconds(86400),
                null
        );
        MvcResult sRes = mockMvc.perform(post("/api/v1/shipments")
                        .with(csrf())
                        .session(sessionA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(shipmentReq)))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode sNode = objectMapper.readTree(sRes.getResponse().getContentAsString());
        Long shipmentAId = sNode.path("data").path("id").asLong();

        // E) Geofence
        GeofenceCreateRequest fenceReq = new GeofenceCreateRequest(
                "Alpha Port Terminal Hub",
                18.9500,
                72.8500,
                800.0,
                "DEPOT",
                "CIRCLE",
                null,
                40.0
        );
        mockMvc.perform(post("/api/v1/tracking/geofences")
                        .with(csrf())
                        .session(sessionA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(fenceReq)))
                .andExpect(status().isCreated());

        // 4. VERIFY TENANT BETA DATA ISOLATION:
        // Beta must NOT see Tenant Alpha's Vehicles
        mockMvc.perform(get("/api/v1/fleet/vehicles").session(sessionB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isEmpty())
                .andExpect(jsonPath("$.data.totalElements").value(0));

        mockMvc.perform(get("/api/v1/fleet/vehicles/" + vehicleAId).session(sessionB))
                .andExpect(status().isNotFound());

        // Beta must NOT see Tenant Alpha's Drivers
        mockMvc.perform(get("/api/v1/trips/drivers").session(sessionB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isEmpty())
                .andExpect(jsonPath("$.data.totalElements").value(0));

        mockMvc.perform(get("/api/v1/trips/drivers/" + driverAId).session(sessionB))
                .andExpect(status().isNotFound());

        // Beta must NOT see Tenant Alpha's Trips
        mockMvc.perform(get("/api/v1/trips").session(sessionB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isEmpty())
                .andExpect(jsonPath("$.data.totalElements").value(0));

        mockMvc.perform(get("/api/v1/trips/" + tripAId).session(sessionB))
                .andExpect(status().isNotFound());

        // Beta must NOT see Tenant Alpha's Shipments
        mockMvc.perform(get("/api/v1/shipments").session(sessionB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isEmpty())
                .andExpect(jsonPath("$.data.totalElements").value(0));

        mockMvc.perform(get("/api/v1/shipments/" + shipmentAId).session(sessionB))
                .andExpect(status().isNotFound());

        // Beta must NOT see Tenant Alpha's Geofences
        mockMvc.perform(get("/api/v1/tracking/geofences").session(sessionB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isEmpty());

        // 5. CROSS-TENANT MUTATION GUARDS:
        // Tenant Beta attempts to create a trip using Tenant Alpha's vehicle & driver -> must be rejected
        TripCreateRequest crossTenantTripReq = new TripCreateRequest(
                vehicleAId,
                driverAId,
                "Beta Origin",
                "Beta Destination",
                Instant.now().plusSeconds(3600),
                Instant.now().plusSeconds(7200),
                java.math.BigDecimal.valueOf(50.0),
                "Cross tenant attempt"
        );
        mockMvc.perform(post("/api/v1/trips")
                        .with(csrf())
                        .session(sessionB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(crossTenantTripReq)))
                .andExpect(status().isBadRequest());

        // 6. CONTROL TOWER KPI ISOLATION:
        // Tenant Beta's summary must show 0 vehicles, 0 trips, 0 shipments
        mockMvc.perform(get("/api/v1/tracking/control-tower/summary").session(sessionB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalVehicles").value(0))
                .andExpect(jsonPath("$.data.ongoingTripsCount").value(0))
                .andExpect(jsonPath("$.data.totalShipmentsCount").value(0));

        // Whereas Tenant Alpha's summary reflects its own assets
        mockMvc.perform(get("/api/v1/tracking/control-tower/summary").session(sessionA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalVehicles").value(1))
                .andExpect(jsonPath("$.data.totalShipmentsCount").value(1));

        // 7. REPORT EXPORT ISOLATION:
        // Tenant Beta exporting CSV report has 0 data rows (only 1 header row)
        MvcResult reportResult = mockMvc.perform(get("/api/v1/tracking/reports/export-csv")
                        .session(sessionB)
                        .param("reportType", "VEHICLES"))
                .andExpect(status().isOk())
                .andReturn();
        String csv = reportResult.getResponse().getContentAsString().trim();
        String[] lines = csv.split("\r\n|\n");
        assertThat(lines).hasSize(1); // Only header line
    }
}
