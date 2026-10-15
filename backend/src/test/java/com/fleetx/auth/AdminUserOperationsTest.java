package com.fleetx.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fleetx.FleetxApplication;
import com.fleetx.auth.dto.AdminPasswordResetRequest;
import com.fleetx.auth.dto.UserCreateRequest;
import com.fleetx.auth.dto.UserStatusUpdateRequest;
import com.fleetx.auth.dto.UserUpdateRequest;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = FleetxApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class AdminUserOperationsTest {

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

    private User adminUser;
    private User driverUser;

    @BeforeEach
    void setupData() {
        auditLogRepository.deleteAll();
        userRepository.deleteAll();

        Role adminRole = roleRepository.findByName("ROLE_ADMIN")
                .orElseGet(() -> roleRepository.save(new Role("ROLE_ADMIN", "Admin")));
        Role driverRole = roleRepository.findByName("ROLE_DRIVER")
                .orElseGet(() -> roleRepository.save(new Role("ROLE_DRIVER", "Driver")));
        roleRepository.findByName("ROLE_DISPATCHER")
                .orElseGet(() -> roleRepository.save(new Role("ROLE_DISPATCHER", "Dispatcher")));

        adminUser = new User("admin@fleetx.test", passwordEncoder.encode("AdminPass123!"), "Primary Admin");
        adminUser.setActive(true);
        adminUser.setRoles(Set.of(adminRole));
        adminUser = userRepository.save(adminUser);

        driverUser = new User("driver@fleetx.test", passwordEncoder.encode("DriverPass123!"), "Test Driver");
        driverUser.setActive(true);
        driverUser.setRoles(Set.of(driverRole));
        driverUser = userRepository.save(driverUser);
    }

    @Test
    @DisplayName("Non-admin user should receive HTTP 403 Forbidden on admin endpoints")
    @WithMockUser(username = "driver@fleetx.test", roles = {"DRIVER"})
    void shouldDenyNonAdminAccess() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("FORBIDDEN"));
    }

    @Test
    @DisplayName("Admin can list users with pagination and search")
    @WithMockUser(username = "admin@fleetx.test", roles = {"ADMIN"})
    void shouldListUsersForAdmin() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users?page=0&size=10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content.length()").value(2))
                .andExpect(jsonPath("$.data.totalElements").value(2));
    }

    @Test
    @DisplayName("Admin can provision a new user with valid parameters")
    @WithMockUser(username = "admin@fleetx.test", roles = {"ADMIN"})
    void shouldCreateNewUser() throws Exception {
        UserCreateRequest request = new UserCreateRequest(
                "new.dispatcher@fleetx.test",
                "New Dispatcher",
                "SecureP@ss123",
                Set.of("DISPATCHER")
        );

        mockMvc.perform(post("/api/v1/admin/users")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value("new.dispatcher@fleetx.test"))
                .andExpect(jsonPath("$.data.roles[0]").value("DISPATCHER"));

        assertThat(userRepository.findByEmail("new.dispatcher@fleetx.test")).isPresent();
        assertThat(auditLogRepository.findAll())
                .anyMatch(l -> "USER_CREATED".equals(l.getAction()));
    }

    @Test
    @DisplayName("User creation fails with 400 when validation constraints fail")
    @WithMockUser(username = "admin@fleetx.test", roles = {"ADMIN"})
    void shouldRejectInvalidUserCreation() throws Exception {
        UserCreateRequest invalid = new UserCreateRequest(
                "not-an-email",
                "",
                "short",
                Set.of()
        );

        mockMvc.perform(post("/api/v1/admin/users")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.validationErrors").isArray());
    }

    @Test
    @DisplayName("User creation fails with 409 when email already exists")
    @WithMockUser(username = "admin@fleetx.test", roles = {"ADMIN"})
    void shouldRejectDuplicateEmailCreation() throws Exception {
        UserCreateRequest duplicate = new UserCreateRequest(
                "driver@fleetx.test",
                "Duplicate Driver",
                "SecurePass123!",
                Set.of("DRIVER")
        );

        mockMvc.perform(post("/api/v1/admin/users")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicate)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("RESOURCE_CONFLICT"));
    }

    @Test
    @DisplayName("Admin can update permitted fields of another user")
    @WithMockUser(username = "admin@fleetx.test", roles = {"ADMIN"})
    void shouldUpdateUser() throws Exception {
        UserUpdateRequest update = new UserUpdateRequest(
                "Updated Driver Name",
                Set.of("DRIVER", "DISPATCHER"),
                true
        );

        mockMvc.perform(put("/api/v1/admin/users/" + driverUser.getId())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.fullName").value("Updated Driver Name"))
                .andExpect(jsonPath("$.data.roles.length()").value(2));

        assertThat(auditLogRepository.findAll())
                .anyMatch(l -> "USER_UPDATED".equals(l.getAction()));
    }

    @Test
    @DisplayName("Admin self-demotion is prevented when they are the sole active admin")
    @WithMockUser(username = "admin@fleetx.test", roles = {"ADMIN"})
    void shouldPreventSoleAdminSelfDemotion() throws Exception {
        UserUpdateRequest selfDemote = new UserUpdateRequest(
                "Primary Admin",
                Set.of("DRIVER"),
                true
        );

        mockMvc.perform(put("/api/v1/admin/users/" + adminUser.getId())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(selfDemote)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("PRIVILEGE_DEMOTION_PREVENTED"));
    }

    @Test
    @DisplayName("Admin self-deactivation is strictly prevented")
    @WithMockUser(username = "admin@fleetx.test", roles = {"ADMIN"})
    void shouldPreventSelfDeactivation() throws Exception {
        UserStatusUpdateRequest deactivateSelf = new UserStatusUpdateRequest(false, "Test self lockout");

        mockMvc.perform(put("/api/v1/admin/users/" + adminUser.getId() + "/status")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(deactivateSelf)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("SELF_DEACTIVATION_PREVENTED"));
    }

    @Test
    @DisplayName("Admin can deactivate a non-admin user")
    @WithMockUser(username = "admin@fleetx.test", roles = {"ADMIN"})
    void shouldDeactivateTargetUser() throws Exception {
        UserStatusUpdateRequest deactivateDriver = new UserStatusUpdateRequest(false, "Safety violation");

        mockMvc.perform(put("/api/v1/admin/users/" + driverUser.getId() + "/status")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(deactivateDriver)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.active").value(false));

        User updated = userRepository.findById(driverUser.getId()).orElseThrow();
        assertThat(updated.isActive()).isFalse();

        assertThat(auditLogRepository.findAll())
                .anyMatch(l -> "USER_DEACTIVATED".equals(l.getAction()));
    }

    @Test
    @DisplayName("Admin can execute password reset workflow for user")
    @WithMockUser(username = "admin@fleetx.test", roles = {"ADMIN"})
    void shouldResetUserPassword() throws Exception {
        AdminPasswordResetRequest reset = new AdminPasswordResetRequest("NewPassword999!");

        mockMvc.perform(put("/api/v1/admin/users/" + driverUser.getId() + "/password-reset")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reset)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        User updated = userRepository.findById(driverUser.getId()).orElseThrow();
        assertThat(passwordEncoder.matches("NewPassword999!", updated.getPasswordHash())).isTrue();

        assertThat(auditLogRepository.findAll())
                .anyMatch(l -> "USER_PASSWORD_RESET".equals(l.getAction()));
    }

    @Test
    @DisplayName("Admin can inspect security audit logs with pagination")
    @WithMockUser(username = "admin@fleetx.test", roles = {"ADMIN"})
    void shouldListAuditLogs() throws Exception {
        mockMvc.perform(get("/api/v1/admin/audit-logs?page=0&size=10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray());
    }
}
