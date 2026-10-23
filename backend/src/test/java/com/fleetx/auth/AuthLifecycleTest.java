package com.fleetx.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fleetx.FleetxApplication;
import com.fleetx.auth.dto.LoginRequest;
import com.fleetx.auth.dto.PasswordChangeRequest;
import com.fleetx.auth.entity.Role;
import com.fleetx.auth.entity.User;
import com.fleetx.auth.repository.AuditLogRepository;
import com.fleetx.auth.repository.RoleRepository;
import com.fleetx.auth.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

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
    private RoleRepository roleRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Role adminRole;
    private Role driverRole;

    @BeforeEach
    void setupTestData() {
        auditLogRepository.deleteAll();
        userRepository.deleteAll();

        adminRole = roleRepository.findByName("ROLE_ADMIN")
                .orElseGet(() -> roleRepository.save(new Role("ROLE_ADMIN", "Admin")));
        driverRole = roleRepository.findByName("ROLE_DRIVER")
                .orElseGet(() -> roleRepository.save(new Role("ROLE_DRIVER", "Driver")));

        User activeAdmin = new User("admin@fleetx.test", passwordEncoder.encode("SecretPass123!"), "Admin User");
        activeAdmin.setActive(true);
        activeAdmin.setRoles(Set.of(adminRole));
        userRepository.save(activeAdmin);

        User deactivatedUser = new User("deactivated@fleetx.test", passwordEncoder.encode("SecretPass123!"), "Deactivated Driver");
        deactivatedUser.setActive(false);
        deactivatedUser.setRoles(Set.of(driverRole));
        userRepository.save(deactivatedUser);
    }

    @Test
    @DisplayName("Valid credentials should authenticate, establish session, and audit success")
    void shouldAuthenticateSuccessfully() throws Exception {
        LoginRequest request = new LoginRequest("admin@fleetx.test", "SecretPass123!");

        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value("admin@fleetx.test"))
                .andExpect(jsonPath("$.data.roles[0]").value("ADMIN"))
                .andReturn();

        MockHttpSession session = (MockHttpSession) result.getRequest().getSession(false);
        assertThat(session).isNotNull();

        // Verify /api/v1/auth/me with session returns user
        mockMvc.perform(get("/api/v1/auth/me").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value("admin@fleetx.test"));

        // Verify audit log recorded
        assertThat(auditLogRepository.findAll())
                .anyMatch(log -> "USER_LOGIN_SUCCESS".equals(log.getAction()));
    }

    @Test
    @DisplayName("Invalid password should return 401 Unauthorized and audit failure")
    void shouldRejectInvalidPassword() throws Exception {
        LoginRequest request = new LoginRequest("admin@fleetx.test", "WrongPassword999!");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));

        assertThat(auditLogRepository.findAll())
                .anyMatch(log -> "USER_LOGIN_FAILED".equals(log.getAction()));
    }

    @Test
    @DisplayName("Deactivated user should be forbidden from logging in")
    void shouldRejectDeactivatedUserLogin() throws Exception {
        LoginRequest request = new LoginRequest("deactivated@fleetx.test", "SecretPass123!");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("ACCOUNT_DEACTIVATED"));

        assertThat(auditLogRepository.findAll())
                .anyMatch(log -> "USER_LOGIN_BLOCKED".equals(log.getAction()));
    }

    @Test
    @DisplayName("Unauthenticated request to /auth/me should return 401")
    void shouldRejectUnauthenticatedMe() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("Logout should invalidate session and prevent subsequent authenticated calls")
    void shouldLogoutSuccessfully() throws Exception {
        LoginRequest request = new LoginRequest("admin@fleetx.test", "SecretPass123!");

        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession(false);
        assertThat(session).isNotNull();

        // Logout with CSRF
        mockMvc.perform(post("/api/v1/auth/logout").session(session).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        assertThat(session.isInvalid()).isTrue();

        // Subsequent /auth/me with invalidated session should be 401
        mockMvc.perform(get("/api/v1/auth/me").session(session))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Mutating request without CSRF should be rejected with 403 Forbidden")
    void shouldRejectMutatingRequestWithoutCsrf() throws Exception {
        LoginRequest request = new LoginRequest("admin@fleetx.test", "SecretPass123!");

        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession(false);

        // Attempt logout without CSRF token
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

    @Test
    @DisplayName("Authenticated user should successfully change password with valid current password")
    void shouldChangePasswordSuccessfullyWhenAuthenticated() throws Exception {
        // 1. Login to establish authenticated session
        LoginRequest loginReq = new LoginRequest("admin@fleetx.test", "SecretPass123!");
        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andReturn();

        MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession(false);
        assertThat(session).isNotNull();

        // 2. Perform password change
        PasswordChangeRequest changeReq = new PasswordChangeRequest(
                "SecretPass123!",
                "NewStrongPass999!",
                "NewStrongPass999!"
        );

        mockMvc.perform(put("/api/v1/auth/password")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(changeReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Password updated successfully."));

        // 3. Verify audit log
        assertThat(auditLogRepository.findAll())
                .anyMatch(l -> "USER_PASSWORD_CHANGED".equals(l.getAction()));

        // 4. Verify login with old password fails
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("admin@fleetx.test", "SecretPass123!"))))
                .andExpect(status().isUnauthorized());

        // 5. Verify login with new password succeeds
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("admin@fleetx.test", "NewStrongPass999!"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value("admin@fleetx.test"));
    }

    @Test
    @DisplayName("Password change should fail when current password is wrong")
    void shouldRejectPasswordChangeWithWrongCurrentPassword() throws Exception {
        LoginRequest loginReq = new LoginRequest("admin@fleetx.test", "SecretPass123!");
        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andReturn();

        MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession(false);

        PasswordChangeRequest changeReq = new PasswordChangeRequest(
                "IncorrectPass!",
                "NewStrongPass999!",
                "NewStrongPass999!"
        );

        mockMvc.perform(put("/api/v1/auth/password")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(changeReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_CURRENT_PASSWORD"));

        assertThat(auditLogRepository.findAll())
                .anyMatch(l -> "USER_PASSWORD_CHANGE_FAILED".equals(l.getAction()));
    }

    @Test
    @DisplayName("Password change should fail when new password and confirmation mismatch")
    void shouldRejectPasswordChangeWithMismatchedConfirmation() throws Exception {
        LoginRequest loginReq = new LoginRequest("admin@fleetx.test", "SecretPass123!");
        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andReturn();

        MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession(false);

        PasswordChangeRequest changeReq = new PasswordChangeRequest(
                "SecretPass123!",
                "NewStrongPass999!",
                "DifferentConfirmation123!"
        );

        mockMvc.perform(put("/api/v1/auth/password")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(changeReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("PASSWORD_MISMATCH"));
    }

    @Test
    @DisplayName("Unauthenticated request to change password should return 401")
    void shouldRejectUnauthenticatedPasswordChange() throws Exception {
        PasswordChangeRequest changeReq = new PasswordChangeRequest(
                "AnyOldPassword!",
                "NewStrongPass999!",
                "NewStrongPass999!"
        );

        mockMvc.perform(put("/api/v1/auth/password")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(changeReq)))
                .andExpect(status().isUnauthorized());
    }
}
