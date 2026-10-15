package com.fleetx.shared;

import com.fleetx.FleetxApplication;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = FleetxApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class SecurityAccessTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Protected fleet endpoint should return HTTP 401 when unauthenticated")
    void shouldDenyUnauthenticatedAccessToFleet() throws Exception {
        mockMvc.perform(get("/api/v1/fleet/vehicles")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));
    }

    @Test
    @DisplayName("Protected trip dispatch endpoint should return HTTP 401 when unauthenticated")
    void shouldDenyUnauthenticatedAccessToTrips() throws Exception {
        mockMvc.perform(get("/api/v1/trips")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));
    }

    @Test
    @DisplayName("Protected shipment manifests endpoint should return HTTP 401 when unauthenticated")
    void shouldDenyUnauthenticatedAccessToShipments() throws Exception {
        mockMvc.perform(get("/api/v1/shipments")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));
    }

    @Test
    @DisplayName("Protected admin users endpoint should return HTTP 401 when unauthenticated")
    void shouldDenyUnauthenticatedAccessToAdminUsers() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));
    }

    @Test
    @DisplayName("Authenticated non-admin role (DISPATCHER) should receive HTTP 403 Forbidden on admin endpoints")
    @WithMockUser(username = "dispatcher@fleetx.test", roles = {"DISPATCHER"})
    void shouldDenyDispatcherFromAdminUsers() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("FORBIDDEN"));
    }

    @Test
    @DisplayName("Authenticated non-admin role (DRIVER) should receive HTTP 403 Forbidden on admin endpoints")
    @WithMockUser(username = "driver@fleetx.test", roles = {"DRIVER"})
    void shouldDenyDriverFromAdminUsers() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("FORBIDDEN"));
    }

    @Test
    @DisplayName("Public health endpoint should be accessible without credentials")
    void shouldAllowPublicAccessToHealthCheck() throws Exception {
        mockMvc.perform(get("/api/health")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}

