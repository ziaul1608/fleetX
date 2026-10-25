package com.fleetx.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fleetx.FleetxApplication;
import com.fleetx.auth.dto.UserCreateRequest;
import com.fleetx.auth.dto.UserStatusUpdateRequest;
import com.fleetx.auth.dto.UserUpdateRequest;
import com.fleetx.auth.entity.Role;
import com.fleetx.auth.entity.Tenant;
import com.fleetx.auth.entity.User;
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
public class AdminCrossTenantStaffIsolationTest {

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

    private Tenant tenantA;
    private Tenant tenantB;

    private User adminA;
    private User staffA;
    private User adminB;
    private User staffB;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();

        tenantA = tenantRepository.findByTenantCode("TNT-A-STAFF")
                .orElseGet(() -> tenantRepository.save(new Tenant("TNT-A-STAFF", "Tenant A Freight", "Warehouse Alpha")));
        tenantB = tenantRepository.findByTenantCode("TNT-B-STAFF")
                .orElseGet(() -> tenantRepository.save(new Tenant("TNT-B-STAFF", "Tenant B Cargo", "Warehouse Beta")));

        Role adminRole = roleRepository.findByName("ROLE_ADMIN")
                .orElseGet(() -> roleRepository.save(new Role("ROLE_ADMIN", "Admin")));
        Role driverRole = roleRepository.findByName("ROLE_DRIVER")
                .orElseGet(() -> roleRepository.save(new Role("ROLE_DRIVER", "Driver")));

        // Tenant A: 1 Admin, 1 Driver
        adminA = new User();
        adminA.setTenantId(tenantA.getId());
        adminA.setEmail("admin@tenant-a.test");
        adminA.setFullName("Admin A");
        adminA.setActive(true);
        adminA.setRoles(Set.of(adminRole));
        adminA = userRepository.save(adminA);

        staffA = new User();
        staffA.setTenantId(tenantA.getId());
        staffA.setEmail("driver@tenant-a.test");
        staffA.setFullName("Driver A");
        staffA.setActive(true);
        staffA.setRoles(Set.of(driverRole));
        staffA = userRepository.save(staffA);

        // Tenant B: 1 Admin, 1 Driver
        adminB = new User();
        adminB.setTenantId(tenantB.getId());
        adminB.setEmail("admin@tenant-b.test");
        adminB.setFullName("Admin B");
        adminB.setActive(true);
        adminB.setRoles(Set.of(adminRole));
        adminB = userRepository.save(adminB);

        staffB = new User();
        staffB.setTenantId(tenantB.getId());
        staffB.setEmail("driver@tenant-b.test");
        staffB.setFullName("Driver B");
        staffB.setActive(true);
        staffB.setRoles(Set.of(driverRole));
        staffB = userRepository.save(staffB);
    }

    private MockHttpSession createSession(User user, Tenant tenant) {
        MockHttpSession session = new MockHttpSession();
        FleetxUserPrincipal principal = new FleetxUserPrincipal(user, tenant);
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(auth);
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);
        return session;
    }

    @Test
    @DisplayName("Admin of Tenant A only sees Tenant A users in directory and cannot see Tenant B users")
    void testTenantLocalUserDirectoryScoping() throws Exception {
        MockHttpSession sessionA = createSession(adminA, tenantA);

        mockMvc.perform(get("/api/v1/admin/users").session(sessionA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(2))
                .andExpect(jsonPath("$.data.content[*].email").value(org.hamcrest.Matchers.containsInAnyOrder(
                        "admin@tenant-a.test", "driver@tenant-a.test"
                )));
    }

    @Test
    @DisplayName("Admin of Tenant A cannot retrieve Tenant B staff details by ID (returns 404)")
    void testCrossTenantUserAccessDenied() throws Exception {
        MockHttpSession sessionA = createSession(adminA, tenantA);

        mockMvc.perform(get("/api/v1/admin/users/" + staffB.getId()).session(sessionA))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Admin of Tenant A cannot update Tenant B staff details (returns 404)")
    void testCrossTenantUserUpdateDenied() throws Exception {
        MockHttpSession sessionA = createSession(adminA, tenantA);

        UserUpdateRequest updateReq = new UserUpdateRequest("Hacked Driver Name", Set.of("DRIVER"), true);

        mockMvc.perform(put("/api/v1/admin/users/" + staffB.getId())
                        .session(sessionA)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Admin of Tenant A cannot deactivate Tenant B staff (returns 404)")
    void testCrossTenantUserDeactivationDenied() throws Exception {
        MockHttpSession sessionA = createSession(adminA, tenantA);

        UserStatusUpdateRequest statusReq = new UserStatusUpdateRequest(false, "Malicious deactivation");

        mockMvc.perform(put("/api/v1/admin/users/" + staffB.getId() + "/status")
                        .session(sessionA)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(statusReq)))
                .andExpect(status().isNotFound());

        // Verify Staff B is still active
        User unchangedB = userRepository.findById(staffB.getId()).orElseThrow();
        assertThat(unchangedB.isActive()).isTrue();
    }

    @Test
    @DisplayName("New staff user provisioned by Admin A is automatically bound to Tenant A")
    void testStaffCreationScoping() throws Exception {
        MockHttpSession sessionA = createSession(adminA, tenantA);

        UserCreateRequest createReq = new UserCreateRequest(
                "new.dispatcher@tenant-a.test",
                "Dispatcher Alpha",
                null,
                Set.of("DISPATCHER")
        );

        mockMvc.perform(post("/api/v1/admin/users")
                        .session(sessionA)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.email").value("new.dispatcher@tenant-a.test"))
                .andExpect(jsonPath("$.data.tenantId").value(tenantA.getId()));

        User created = userRepository.findByEmail("new.dispatcher@tenant-a.test").orElseThrow();
        assertThat(created.getTenantId()).isEqualTo(tenantA.getId());
        assertThat(created.getPasswordHash()).isNull(); // Password auth retired
    }
}
