package com.fleetx.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fleetx.FleetxApplication;
import com.fleetx.auth.dto.LoginRequest;
import com.fleetx.auth.dto.PasswordChangeRequest;
import com.fleetx.auth.entity.Role;
import com.fleetx.auth.entity.Tenant;
import com.fleetx.auth.entity.User;
import com.fleetx.auth.repository.AuditLogRepository;
import com.fleetx.auth.repository.RoleRepository;
import com.fleetx.auth.repository.TenantRepository;
import com.fleetx.auth.repository.UserRepository;
import com.fleetx.auth.security.FleetxUserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = FleetxApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class AuthLifecycleTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    private User activeAdmin;
    private Tenant tenant;

    @BeforeEach
    void setupTestData() {
        auditLogRepository.deleteAll();
        userRepository.deleteAll();

        tenant = tenantRepository.findByTenantCode("TNT-AUTH-TEST")
                .orElseGet(() -> tenantRepository.save(new Tenant("TNT-AUTH-TEST", "Auth Test Logistics", "Test Central Hub")));

        Role adminRole = roleRepository.findByName("ROLE_ADMIN")
                .orElseGet(() -> roleRepository.save(new Role("ROLE_ADMIN", "Admin")));

        activeAdmin = new User();
        activeAdmin.setTenantId(tenant.getId());
        activeAdmin.setEmail("admin@fleetx.test");
        activeAdmin.setFullName("Admin User");
        activeAdmin.setActive(true);
        activeAdmin.setRoles(Set.of(adminRole));
        activeAdmin = userRepository.save(activeAdmin);
    }

    private MockHttpSession createAuthenticatedSession(User user, Tenant userTenant) {
        MockHttpSession session = new MockHttpSession();
        FleetxUserPrincipal principal = new FleetxUserPrincipal(user, userTenant);
        UsernamePasswordAuthenticationToken authToken =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authToken);
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);
        return session;
    }

    @Test
    @DisplayName("Password-only login must be rejected with PASSWORD_AUTH_RETIRED")
    void shouldRejectPasswordLoginAsRetired() throws Exception {
        LoginRequest request = new LoginRequest("admin@fleetx.test", "AnyPassword123!");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("PASSWORD_AUTH_RETIRED"))
                .andExpect(jsonPath("$.message").value("Password-only authentication has been retired. Please log in using your registered email and a 6-digit verification code."));
    }

    @Test
    @DisplayName("Password change endpoint must be rejected with PASSWORD_AUTH_RETIRED")
    void shouldRejectPasswordChangeAsRetired() throws Exception {
        MockHttpSession session = createAuthenticatedSession(activeAdmin, tenant);
        PasswordChangeRequest changeReq = new PasswordChangeRequest(
                "CurrentPass123!",
                "NewStrongPass999!",
                "NewStrongPass999!"
        );

        mockMvc.perform(put("/api/v1/auth/password")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(changeReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("PASSWORD_AUTH_RETIRED"))
                .andExpect(jsonPath("$.message").value("Password management has been retired. FleetX uses passwordless Email OTP authentication."));
    }

    @Test
    @DisplayName("Unauthenticated request to /auth/me should return 401")
    void shouldRejectUnauthenticatedMe() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("Authenticated session retrieves user details via /auth/me")
    void shouldRetrieveCurrentUserSession() throws Exception {
        MockHttpSession session = createAuthenticatedSession(activeAdmin, tenant);

        mockMvc.perform(get("/api/v1/auth/me").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value("admin@fleetx.test"))
                .andExpect(jsonPath("$.data.companyName").value("Auth Test Logistics"))
                .andExpect(jsonPath("$.data.warehouseName").value("Test Central Hub"))
                .andExpect(jsonPath("$.data.roles[0]").value("ADMIN"));
    }

    @Test
    @DisplayName("Logout should invalidate session and prevent subsequent authenticated calls")
    void shouldLogoutSuccessfully() throws Exception {
        MockHttpSession session = createAuthenticatedSession(activeAdmin, tenant);

        mockMvc.perform(post("/api/v1/auth/logout").session(session).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        assertThat(session.isInvalid()).isTrue();

        mockMvc.perform(get("/api/v1/auth/me").session(session))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Mutating request without CSRF should be rejected with 403 Forbidden")
    void shouldRejectMutatingRequestWithoutCsrf() throws Exception {
        MockHttpSession session = createAuthenticatedSession(activeAdmin, tenant);

        mockMvc.perform(post("/api/v1/auth/logout").session(session))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("CSRF bootstrap endpoint should return token metadata")
    void shouldProvideCsrfBootstrap() throws Exception {
        mockMvc.perform(get("/api/v1/auth/csrf"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.headerName").value("X-CSRF-TOKEN"));
    }
}
