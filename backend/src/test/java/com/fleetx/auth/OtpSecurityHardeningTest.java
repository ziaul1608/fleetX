package com.fleetx.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fleetx.FleetxApplication;
import com.fleetx.auth.dto.SendLoginOtpRequest;
import com.fleetx.auth.dto.VerifyLoginOtpRequest;
import com.fleetx.auth.entity.Role;
import com.fleetx.auth.entity.Tenant;
import com.fleetx.auth.entity.User;
import com.fleetx.auth.repository.OtpChallengeRepository;
import com.fleetx.auth.repository.RoleRepository;
import com.fleetx.auth.repository.TenantRepository;
import com.fleetx.auth.repository.UserRepository;
import com.fleetx.auth.service.MockEmailService;
import com.fleetx.auth.service.OtpService;
import com.fleetx.shared.exception.DomainException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = FleetxApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class OtpSecurityHardeningTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OtpService otpService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private OtpChallengeRepository otpChallengeRepository;

    @Autowired
    private MockEmailService mockEmailService;

    private Tenant activeTenant;
    private Tenant suspendedTenant;
    private User activeUser;
    private User deactivatedUser;

    @BeforeEach
    void setUp() {
        mockEmailService.clear();
        otpChallengeRepository.deleteAll();
        userRepository.deleteAll();

        activeTenant = tenantRepository.findByTenantCode("TNT-ACTIVE-HARDEN")
                .orElseGet(() -> tenantRepository.save(new Tenant("TNT-ACTIVE-HARDEN", "Active Logistics Co", "Active Hub")));
        suspendedTenant = tenantRepository.findByTenantCode("TNT-SUSPENDED")
                .orElseGet(() -> {
                    Tenant t = new Tenant("TNT-SUSPENDED", "Suspended Logistics", "Suspended Hub");
                    t.setStatus("SUSPENDED");
                    return tenantRepository.save(t);
                });

        Role adminRole = roleRepository.findByName("ROLE_ADMIN")
                .orElseGet(() -> roleRepository.save(new Role("ROLE_ADMIN", "Admin")));

        activeUser = new User();
        activeUser.setTenantId(activeTenant.getId());
        activeUser.setEmail("active.owner@warehouse.test");
        activeUser.setFullName("Active Owner");
        activeUser.setActive(true);
        activeUser.setRoles(Set.of(adminRole));
        activeUser = userRepository.save(activeUser);

        deactivatedUser = new User();
        deactivatedUser.setTenantId(activeTenant.getId());
        deactivatedUser.setEmail("deactivated.owner@warehouse.test");
        deactivatedUser.setFullName("Deactivated Owner");
        deactivatedUser.setActive(false);
        deactivatedUser.setRoles(Set.of(adminRole));
        deactivatedUser = userRepository.save(deactivatedUser);
    }

    @Test
    @DisplayName("HMAC-SHA-256 calculation binds to email address and changes with different secret keys")
    void testHmacSha256BindingAndSecretSensitivity() {
        String hash1 = otpService.computeHmacHex("user1@warehouse.test", "123456");
        String hash2 = otpService.computeHmacHex("user2@warehouse.test", "123456");
        assertThat(hash1).isNotEqualTo(hash2);

        // Test with different secret key
        String originalSecret = (String) ReflectionTestUtils.getField(otpService, "hmacSecret");
        try {
            ReflectionTestUtils.setField(otpService, "hmacSecret", "alternative-secret-key-for-test-32bytes!");
            String hash3 = otpService.computeHmacHex("user1@warehouse.test", "123456");
            assertThat(hash3).isNotEqualTo(hash1);
        } finally {
            ReflectionTestUtils.setField(otpService, "hmacSecret", originalSecret);
        }
    }

    @Test
    @DisplayName("Email enumeration defense: send-login returns uniform 200 response for non-existent email without dispatching OTP")
    void testEmailEnumerationDefenseForNonExistentEmail() throws Exception {
        SendLoginOtpRequest request = new SendLoginOtpRequest("nonexistent.random.user99@unknown.test");

        mockMvc.perform(post("/api/v1/auth/otp/send-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value("nonexistent.random.user99@unknown.test"))
                .andExpect(jsonPath("$.data.purpose").value("LOGIN"))
                .andExpect(jsonPath("$.data.message").value("If an account is associated with this email address, a verification code has been dispatched."));

        // Verify NO OTP was dispatched or recorded in mockEmailService
        assertThat(mockEmailService.getLastSentOtp("nonexistent.random.user99@unknown.test")).isEmpty();
    }

    @Test
    @DisplayName("Email enumeration defense: send-login returns uniform 200 response for deactivated user without dispatching OTP")
    void testEmailEnumerationDefenseForDeactivatedUser() throws Exception {
        SendLoginOtpRequest request = new SendLoginOtpRequest("deactivated.owner@warehouse.test");

        mockMvc.perform(post("/api/v1/auth/otp/send-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.message").value("If an account is associated with this email address, a verification code has been dispatched."));

        // Verify NO OTP was dispatched to deactivated user
        assertThat(mockEmailService.getLastSentOtp("deactivated.owner@warehouse.test")).isEmpty();
    }

    @Test
    @DisplayName("Single-use consumption: OTP token cannot be verified more than once")
    void testOtpSingleUseConsumption() throws Exception {
        // 1. Send Login OTP
        SendLoginOtpRequest sendReq = new SendLoginOtpRequest("active.owner@warehouse.test");
        mockMvc.perform(post("/api/v1/auth/otp/send-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sendReq)))
                .andExpect(status().isOk());

        String otp = mockEmailService.getLastSentOtp("active.owner@warehouse.test").orElseThrow();

        // 2. First verification succeeds
        VerifyLoginOtpRequest verifyReq = new VerifyLoginOtpRequest("active.owner@warehouse.test", otp);
        mockMvc.perform(post("/api/v1/auth/otp/verify-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(verifyReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // 3. Second verification of the exact same OTP code is blocked (already consumed)
        mockMvc.perform(post("/api/v1/auth/otp/verify-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(verifyReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("OTP_ALREADY_USED"));
    }

    @Test
    @DisplayName("Suspended tenant user is blocked during login verification with TENANT_INACTIVE")
    void testSuspendedTenantLoginBlocked() throws Exception {
        Role adminRole = roleRepository.findByName("ROLE_ADMIN").orElseThrow();
        User suspendedTenantUser = new User();
        suspendedTenantUser.setTenantId(suspendedTenant.getId());
        suspendedTenantUser.setEmail("suspended.owner@warehouse.test");
        suspendedTenantUser.setFullName("Suspended Owner");
        suspendedTenantUser.setActive(true);
        suspendedTenantUser.setRoles(Set.of(adminRole));
        userRepository.save(suspendedTenantUser);

        // Send Login OTP
        SendLoginOtpRequest sendReq = new SendLoginOtpRequest("suspended.owner@warehouse.test");
        mockMvc.perform(post("/api/v1/auth/otp/send-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sendReq)))
                .andExpect(status().isOk());

        String otp = mockEmailService.getLastSentOtp("suspended.owner@warehouse.test").orElseThrow();

        // Verify Login OTP
        VerifyLoginOtpRequest verifyReq = new VerifyLoginOtpRequest("suspended.owner@warehouse.test", otp);
        mockMvc.perform(post("/api/v1/auth/otp/verify-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(verifyReq)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("TENANT_INACTIVE"));
    }
}
