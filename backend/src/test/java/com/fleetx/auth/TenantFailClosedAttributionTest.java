package com.fleetx.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fleetx.FleetxApplication;
import com.fleetx.auth.dto.SendSignupOtpRequest;
import com.fleetx.auth.dto.UserResponseDto;
import com.fleetx.auth.dto.VerifySignupOtpRequest;
import com.fleetx.auth.entity.Role;
import com.fleetx.auth.entity.Tenant;
import com.fleetx.auth.entity.User;
import com.fleetx.auth.repository.RoleRepository;
import com.fleetx.auth.repository.TenantRepository;
import com.fleetx.auth.repository.UserRepository;
import com.fleetx.auth.security.FleetxUserPrincipal;
import com.fleetx.auth.service.MockEmailService;
import com.fleetx.drivers.dto.DriverCreateRequest;
import com.fleetx.drivers.entity.Driver;
import com.fleetx.drivers.repository.DriverRepository;
import com.fleetx.drivers.service.DriverService;
import com.fleetx.fleet.dto.VehicleCreateRequest;
import com.fleetx.fleet.dto.VehicleResponseDto;
import com.fleetx.fleet.dto.VehicleUpdateRequest;
import com.fleetx.fleet.entity.Vehicle;
import com.fleetx.fleet.repository.VehicleRepository;
import com.fleetx.fleet.service.VehicleService;
import com.fleetx.shared.context.TenantSecurityContext;
import com.fleetx.shared.exception.DomainException;
import com.fleetx.shared.exception.ResourceNotFoundException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = FleetxApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class TenantFailClosedAttributionTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private VehicleService vehicleService;

    @Autowired
    private VehicleRepository vehicleRepository;

    @Autowired
    private DriverService driverService;

    @Autowired
    private DriverRepository driverRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private MockEmailService mockEmailService;

    private Tenant tenantAlpha;
    private Tenant tenantBeta;

    @BeforeEach
    void setUp() {
        mockEmailService.clear();

        if (!tenantRepository.existsById(1L)) {
            tenantRepository.save(new Tenant("TNT-GLOBAL-001", "FleetX Global Logistics", "Central Warehouse"));
        }

        tenantAlpha = tenantRepository.findByTenantCode("TNT-ALPHA-FAILCLOSED")
                .orElseGet(() -> tenantRepository.save(new Tenant("TNT-ALPHA-FAILCLOSED", "Alpha Freight Corp", "Alpha Depot")));
        tenantBeta = tenantRepository.findByTenantCode("TNT-BETA-FAILCLOSED")
                .orElseGet(() -> tenantRepository.save(new Tenant("TNT-BETA-FAILCLOSED", "Beta Logistics Inc", "Beta Terminal")));
    }

    @AfterEach
    void tearDown() {
        TenantSecurityContext.clear();
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Requirement 1: Normal tenant-owned insert without context fails fail-closed (TENANT_CONTEXT_REQUIRED, HTTP 400)")
    void normalTenantOwnedInsertWithoutContextFails() {
        TenantSecurityContext.clear();
        SecurityContextHolder.clearContext();

        // 1a. Direct entity persistence fails at @PrePersist
        Vehicle unmanagedVehicle = new Vehicle(
                "1HGCR2F83HA999001", "MH-12-FC-0001", "Tata", "Prima", 2025,
                "TRUCK", "DIESEL", 25000.0, 48.0, 1000.0, "AVAILABLE"
        );
        assertThatThrownBy(() -> vehicleRepository.save(unmanagedVehicle))
                .isInstanceOf(DomainException.class)
                .satisfies(ex -> {
                    DomainException de = (DomainException) ex;
                    assertThat(de.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(de.getErrorCode()).isEqualTo("TENANT_CONTEXT_REQUIRED");
                });

        // 1b. Service-layer creation fails fail-closed when caller has no tenant context
        VehicleCreateRequest vehicleReq = new VehicleCreateRequest(
                "1HGCR2F83HA999002", "MH-12-FC-0002", "Tata", "Prima", 2025,
                "TRUCK", "DIESEL", 25000.0, 48.0, 1000.0, "AVAILABLE"
        );
        assertThatThrownBy(() -> vehicleService.createVehicle(vehicleReq))
                .isInstanceOf(DomainException.class)
                .satisfies(ex -> {
                    DomainException de = (DomainException) ex;
                    assertThat(de.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED)
                            .describedAs("Without authentication, service rejects with UNAUTHORIZED");
                });

        // 1c. Driver entity persistence without context fails
        Driver unmanagedDriver = new Driver();
        unmanagedDriver.setLicenseNumber("DL-FAILCLOSED-001");
        unmanagedDriver.setFullName("Unmanaged Driver");
        unmanagedDriver.setLicenseExpiryDate(LocalDate.now().plusYears(2));
        assertThatThrownBy(() -> driverRepository.save(unmanagedDriver))
                .isInstanceOf(DomainException.class)
                .satisfies(ex -> {
                    DomainException de = (DomainException) ex;
                    assertThat(de.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(de.getErrorCode()).isEqualTo("TENANT_CONTEXT_REQUIRED");
                });
    }

    @Test
    @DisplayName("Requirement 2: Mismatched caller-supplied tenant ID is rejected fail-closed (TENANT_ACCESS_DENIED, HTTP 403)")
    void mismatchedCallerSuppliedTenantIdRejected() {
        // Authenticated as Tenant Alpha
        TenantSecurityContext.setTenantId(tenantAlpha.getId());

        // Attempt to persist an entity specifying Tenant Beta's ID
        Vehicle mismatchedVehicle = new Vehicle(
                "1HGCR2F83HA999003", "MH-12-FC-0003", "Tata", "Prima", 2025,
                "TRUCK", "DIESEL", 25000.0, 48.0, 1000.0, "AVAILABLE"
        );
        mismatchedVehicle.setTenantId(tenantBeta.getId());

        assertThatThrownBy(() -> vehicleRepository.save(mismatchedVehicle))
                .isInstanceOf(DomainException.class)
                .satisfies(ex -> {
                    DomainException de = (DomainException) ex;
                    assertThat(de.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
                    assertThat(de.getErrorCode()).isEqualTo("TENANT_ACCESS_DENIED");
                    assertThat(de.getMessage()).contains("Mismatched tenant ID");
                });

        // Attempting to reassign an existing entity to another tenant fails at @PreUpdate
        Vehicle validVehicle = TenantSecurityContext.executeWithTenant(tenantAlpha.getId(), () -> {
            Vehicle v = new Vehicle(
                    "1HGCR2F83HA999004", "MH-12-FC-0004", "Tata", "Prima", 2025,
                    "TRUCK", "DIESEL", 25000.0, 48.0, 1000.0, "AVAILABLE"
            );
            return vehicleRepository.save(v);
        });

        // Switch context to Tenant Beta and attempt to modify / update Tenant Alpha's entity
        TenantSecurityContext.setTenantId(tenantBeta.getId());
        validVehicle.setMake("Tampered Make");
        assertThatThrownBy(() -> vehicleRepository.save(validVehicle))
                .hasRootCauseInstanceOf(DomainException.class)
                .rootCause()
                .satisfies(ex -> {
                    DomainException de = (DomainException) ex;
                    assertThat(de.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
                    assertThat(de.getErrorCode()).isEqualTo("TENANT_ACCESS_DENIED");
                    assertThat(de.getMessage()).contains("Cannot reassign");
                });
    }

    @Test
    @DisplayName("Requirement 3: Cross-tenant isolation - Tenant A cannot read, update, or assign Tenant B resources")
    void tenantACannotAccessTenantBResources() {
        // Create Vehicle and Driver under Tenant Beta
        VehicleResponseDto betaDto = TenantSecurityContext.executeWithTenant(tenantBeta.getId(), () -> {
            return vehicleService.createVehicle(new VehicleCreateRequest(
                    "1HGCR2F83HA999005", "MH-12-FC-0005", "Tata", "Prima", 2025,
                    "TRUCK", "DIESEL", 25000.0, 48.0, 1000.0, "AVAILABLE"
            ));
        });
        Vehicle vehicleBeta = vehicleRepository.findById(betaDto.id()).orElseThrow();

        // Now authenticate as Tenant Alpha
        TenantSecurityContext.setTenantId(tenantAlpha.getId());

        // 3a. Read isolation: Lookup of Tenant Beta vehicle from Tenant Alpha throws ResourceNotFoundException
        assertThatThrownBy(() -> vehicleService.getVehicleById(vehicleBeta.getId()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Vehicle with identifier '" + vehicleBeta.getId() + "' was not found.");

        // 3b. Update isolation: Tenant Alpha cannot update Tenant Beta vehicle
        assertThatThrownBy(() -> vehicleService.updateVehicle(vehicleBeta.getId(), new VehicleUpdateRequest(
                "Tata", "Prima", 2025, "TRUCK", "DIESEL", 25000.0, 48.0, "MAINTENANCE", true
        )))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Vehicle with identifier '" + vehicleBeta.getId() + "' was not found.");

        // 3c. List isolation: Tenant Alpha vehicle search returns 0 results for Tenant Beta's vehicle
        var searchPage = vehicleService.findVehicles(null, null, null, "MH-12-FC-0005", org.springframework.data.domain.PageRequest.of(0, 10));
        assertThat(searchPage.getContent()).isEmpty();
    }

    @Test
    @DisplayName("Requirement 4: Tenant context is properly established and restored across web requests")
    void tenantContextFilterCleansUpAcrossRequests() throws ServletException, IOException {
        TenantSecurityContext.clear();
        SecurityContextHolder.clearContext();
        assertThat(TenantSecurityContext.getOptionalTenantId()).isEmpty();

        // Build mock principal with Tenant Alpha
        Role adminRole = roleRepository.findByName("ROLE_ADMIN").orElseThrow();
        User alphaUser = TenantSecurityContext.executeWithTenant(tenantAlpha.getId(), () -> {
            User u = new User();
            u.setTenantId(tenantAlpha.getId());
            u.setEmail("filter.test@alpha.test");
            u.setFullName("Filter Test User");
            u.setActive(true);
            u.setRoles(Set.of(adminRole));
            return userRepository.save(u);
        });

        FleetxUserPrincipal principal = new FleetxUserPrincipal(alphaUser, tenantAlpha);
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(principal, "password", principal.getAuthorities());

        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(auth);
        SecurityContextHolder.setContext(securityContext);

        AtomicReference<Long> capturedTenantInsideFilter = new AtomicReference<>();
        AtomicReference<Long> capturedTenantAfterFilter = new AtomicReference<>();

        // Simulate request through the filter chain
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/fleet/vehicles");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = (req, res) -> {
            // Inside the filter: tenant should be established from principal
            capturedTenantInsideFilter.set(TenantSecurityContext.getCurrentTenantId());
        };

        // Run filter manually
        org.springframework.web.filter.OncePerRequestFilter filter =
                (org.springframework.web.filter.OncePerRequestFilter) getTenantContextFilter();
        filter.doFilter(request, response, chain);

        // Outside filter: explicit thread override must be restored to previous (null)
        capturedTenantAfterFilter.set(TenantSecurityContext.getExplicitTenantOverride());

        // Cleanup security context at end of request
        SecurityContextHolder.clearContext();

        assertThat(capturedTenantInsideFilter.get()).isEqualTo(tenantAlpha.getId());
        assertThat(capturedTenantAfterFilter.get()).isNull();
        assertThat(TenantSecurityContext.getOptionalTenantId()).isEmpty();
    }

    @Test
    @DisplayName("Requirement 5: Background simulator and workers cannot write records without explicit trusted context")
    void backgroundWorkersCannotWriteWithoutExplicitContext() throws InterruptedException {
        AtomicReference<Throwable> threadError = new AtomicReference<>();

        Thread worker = new Thread(() -> {
            try {
                // Background worker thread has NO tenant context
                Vehicle v = new Vehicle(
                        "1HGCR2F83HA999006", "MH-12-FC-0006", "Tata", "Prima", 2025,
                        "TRUCK", "DIESEL", 25000.0, 48.0, 1000.0, "AVAILABLE"
                );
                vehicleRepository.save(v);
            } catch (Throwable t) {
                threadError.set(t);
            }
        });
        worker.start();
        worker.join();

        assertThat(threadError.get())
                .isNotNull()
                .isInstanceOf(DomainException.class)
                .satisfies(ex -> {
                    DomainException de = (DomainException) ex;
                    assertThat(de.getErrorCode()).isEqualTo("TENANT_CONTEXT_REQUIRED");
                });

        // Now verify that executeWithTenant works cleanly in background workers
        AtomicReference<Vehicle> savedInWorker = new AtomicReference<>();
        Thread workerWithContext = new Thread(() -> {
            Vehicle v = TenantSecurityContext.executeWithTenant(tenantBeta.getId(), () -> {
                Vehicle newV = new Vehicle(
                        "1HGCR2F83HA999007", "MH-12-FC-0007", "Tata", "Prima", 2025,
                        "TRUCK", "DIESEL", 25000.0, 48.0, 1000.0, "AVAILABLE"
                );
                return vehicleRepository.save(newV);
            });
            savedInWorker.set(v);
        });
        workerWithContext.start();
        workerWithContext.join();

        assertThat(savedInWorker.get()).isNotNull();
        assertThat(savedInWorker.get().getTenantId()).isEqualTo(tenantBeta.getId());
    }

    @Test
    @DisplayName("Requirement 6: Legitimate tenant registration creates new tenant and initial admin without tenant 1 fallback")
    void legitimateTenantRegistrationCreatesIsolatedTenantWithoutTenant1Fallback() throws Exception {
        String email = "new.warehouse.owner@saas-test.com";
        String company = "Apex Logistics Hub";
        String warehouse = "Bhiwandi Sector 4";
        String owner = "Vikram Singhania";

        // Step 1: Send signup OTP
        mockMvc.perform(post("/api/v1/auth/otp/send-signup")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SendSignupOtpRequest(email, company, warehouse, owner))))
                .andExpect(status().isOk());

        String otp = mockEmailService.getLastSentOtp(email).orElseThrow();

        // Step 2: Verify signup OTP
        MvcResult result = mockMvc.perform(post("/api/v1/auth/otp/verify-signup")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new VerifySignupOtpRequest(email, otp, company, warehouse, owner))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value(email))
                .andExpect(jsonPath("$.data.companyName").value(company))
                .andExpect(jsonPath("$.data.warehouseName").value(warehouse))
                .andReturn();

        // Step 3: Verify the newly registered user and tenant in database
        User createdUser = userRepository.findByEmail(email).orElseThrow();
        assertThat(createdUser.getTenantId()).isNotNull();
        assertThat(createdUser.getTenantId()).isNotEqualTo(1L)
                .describedAs("New tenant user MUST NOT fall back to legacy tenant 1");

        Tenant createdTenant = tenantRepository.findById(createdUser.getTenantId()).orElseThrow();
        assertThat(createdTenant.getCompanyName()).isEqualTo(company);
        assertThat(createdTenant.getWarehouseName()).isEqualTo(warehouse);
        assertThat(createdTenant.getStatus()).isEqualTo("ACTIVE");
    }

    @Test
    @DisplayName("Requirement 7: Existing legacy tenant records remain accessible only to authorized legacy tenant")
    void legacyTenantRecordsAccessibleOnlyToAuthorizedLegacyTenant() {
        // Create a record owned by legacy Tenant 1
        VehicleResponseDto legacyDto = TenantSecurityContext.executeWithTenant(1L, () -> {
            return vehicleService.createVehicle(new VehicleCreateRequest(
                    "1HGCR2F83HA999008", "MH-12-LEGACY-01", "Tata", "Prima", 2025,
                    "TRUCK", "DIESEL", 25000.0, 48.0, 1000.0, "AVAILABLE"
            ));
        });
        Vehicle legacyVehicle = vehicleRepository.findById(legacyDto.id()).orElseThrow();

        // 7a. Accessible when authenticated as Tenant 1
        TenantSecurityContext.setTenantId(1L);
        VehicleResponseDto fetchedByLegacy = vehicleService.getVehicleById(legacyVehicle.getId());
        assertThat(fetchedByLegacy).isNotNull();
        assertThat(fetchedByLegacy.licensePlate()).isEqualTo("MH-12-LEGACY-01");

        // 7b. Inaccessible when authenticated as Tenant Alpha
        TenantSecurityContext.setTenantId(tenantAlpha.getId());
        assertThatThrownBy(() -> vehicleService.getVehicleById(legacyVehicle.getId()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Vehicle with identifier '" + legacyVehicle.getId() + "' was not found.");

        // 7c. Inaccessible when authenticated as Tenant Beta
        TenantSecurityContext.setTenantId(tenantBeta.getId());
        assertThatThrownBy(() -> vehicleService.getVehicleById(legacyVehicle.getId()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Vehicle with identifier '" + legacyVehicle.getId() + "' was not found.");
    }

    private Object getTenantContextFilter() {
        try {
            Class<?> clazz = Class.forName("com.fleetx.shared.config.TenantContextFilter");
            var constructor = clazz.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (Exception e) {
            throw new RuntimeException("Could not instantiate TenantContextFilter", e);
        }
    }
}
