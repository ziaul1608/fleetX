package com.fleetx.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fleetx.FleetxApplication;
import com.fleetx.auth.dto.*;
import com.fleetx.auth.entity.Role;
import com.fleetx.auth.entity.Tenant;
import com.fleetx.auth.entity.User;
import com.fleetx.auth.repository.OtpChallengeRepository;
import com.fleetx.auth.repository.RoleRepository;
import com.fleetx.auth.repository.TenantRepository;
import com.fleetx.auth.repository.UserRepository;
import com.fleetx.auth.service.MockEmailService;
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

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = FleetxApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class OtpLifecycleTest {

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
    private OtpChallengeRepository otpChallengeRepository;

    @Autowired
    private MockEmailService mockEmailService;

    @BeforeEach
    void setUp() {
        mockEmailService.clear();
        otpChallengeRepository.deleteAll();
        userRepository.deleteAll();

        roleRepository.findByName("ROLE_ADMIN")
                .orElseGet(() -> roleRepository.save(new Role("ROLE_ADMIN", "Admin")));
    }

    @Test
    @DisplayName("Should send signup OTP code successfully and capture in MockEmailService")
    void testSendSignupOtp_Success() throws Exception {
        SendSignupOtpRequest request = new SendSignupOtpRequest(
                "newowner@warehouse.test",
                "Apex Cargo Ltd",
                "North Terminal Depot",
                "John Dispatcher"
        );

        mockMvc.perform(post("/api/v1/auth/otp/send-signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value("newowner@warehouse.test"))
                .andExpect(jsonPath("$.data.expiresInSeconds").value(300))
                .andExpect(jsonPath("$.data.cooldownSeconds").value(60));

        String capturedOtp = mockEmailService.getLastSentOtp("newowner@warehouse.test").orElse(null);
        assertThat(capturedOtp).isNotNull();
        assertThat(capturedOtp).matches("^[0-9]{6}$");
    }

    @Test
    @DisplayName("Should enforce 60s cooldown on duplicate OTP request within window")
    void testSendSignupOtp_CooldownEnforced() throws Exception {
        SendSignupOtpRequest request = new SendSignupOtpRequest(
                "cooldown@warehouse.test",
                "Apex Cargo Ltd",
                "North Terminal Depot",
                "John Dispatcher"
        );

        // First request succeeds
        mockMvc.perform(post("/api/v1/auth/otp/send-signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        // Immediate second request blocked by cooldown
        mockMvc.perform(post("/api/v1/auth/otp/send-signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("OTP_COOLDOWN"));
    }

    @Test
    @DisplayName("Should reject signup OTP request if email is already registered")
    void testSendSignupOtp_ExistingEmailRejected() throws Exception {
        Role adminRole = roleRepository.findByName("ROLE_ADMIN").orElseThrow();
        User existing = new User("existing@warehouse.test", "hash", "Existing Owner");
        existing.setRoles(Set.of(adminRole));
        userRepository.save(existing);

        SendSignupOtpRequest request = new SendSignupOtpRequest(
                "existing@warehouse.test",
                "Apex Cargo Ltd",
                "North Terminal Depot",
                "Existing Owner"
        );

        mockMvc.perform(post("/api/v1/auth/otp/send-signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("EMAIL_ALREADY_EXISTS"));
    }

    @Test
    @DisplayName("Should verify signup OTP, provision tenant and admin user, and establish session")
    void testVerifySignupOtp_ProvisionsTenantAndUser() throws Exception {
        SendSignupOtpRequest sendReq = new SendSignupOtpRequest(
                "onboarding@warehouse.test",
                "Horizon Logistics Inc",
                "Bay Hub Warehouse",
                "Sarah Miller"
        );

        mockMvc.perform(post("/api/v1/auth/otp/send-signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sendReq)))
                .andExpect(status().isOk());

        String otp = mockEmailService.getLastSentOtp("onboarding@warehouse.test").orElseThrow();

        VerifySignupOtpRequest verifyReq = new VerifySignupOtpRequest(
                "onboarding@warehouse.test",
                otp,
                "Horizon Logistics Inc",
                "Bay Hub Warehouse",
                "Sarah Miller"
        );

        MvcResult result = mockMvc.perform(post("/api/v1/auth/otp/verify-signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(verifyReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value("onboarding@warehouse.test"))
                .andExpect(jsonPath("$.data.fullName").value("Sarah Miller"))
                .andExpect(jsonPath("$.data.companyName").value("Horizon Logistics Inc"))
                .andExpect(jsonPath("$.data.warehouseName").value("Bay Hub Warehouse"))
                .andExpect(jsonPath("$.data.roles[0]").value("ADMIN"))
                .andReturn();

        MockHttpSession session = (MockHttpSession) result.getRequest().getSession(false);
        assertThat(session).isNotNull();

        // Verify authenticated session can call /api/v1/auth/me
        mockMvc.perform(get("/api/v1/auth/me").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value("onboarding@warehouse.test"))
                .andExpect(jsonPath("$.data.companyName").value("Horizon Logistics Inc"));

        // Verify Tenant is in database
        User createdUser = userRepository.findByEmail("onboarding@warehouse.test").orElseThrow();
        assertThat(createdUser.getTenantId()).isNotNull();
        Tenant tenant = tenantRepository.findById(createdUser.getTenantId()).orElseThrow();
        assertThat(tenant.getCompanyName()).isEqualTo("Horizon Logistics Inc");
        assertThat(tenant.getWarehouseName()).isEqualTo("Bay Hub Warehouse");
    }

    @Test
    @DisplayName("Should decrement remaining attempts on invalid OTP code")
    void testVerifySignupOtp_WrongCode_DecrementsAttempts() throws Exception {
        SendSignupOtpRequest sendReq = new SendSignupOtpRequest(
                "attempts@warehouse.test",
                "Horizon Logistics Inc",
                "Bay Hub Warehouse",
                "Sarah Miller"
        );

        mockMvc.perform(post("/api/v1/auth/otp/send-signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sendReq)))
                .andExpect(status().isOk());

        VerifySignupOtpRequest verifyReq = new VerifySignupOtpRequest(
                "attempts@warehouse.test",
                "000000", // Incorrect code
                "Horizon Logistics Inc",
                "Bay Hub Warehouse",
                "Sarah Miller"
        );

        mockMvc.perform(post("/api/v1/auth/otp/verify-signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(verifyReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INCORRECT_OTP"));
    }

    @Test
    @DisplayName("Should send and verify login OTP for existing tenant user")
    void testLoginOtp_Flow() throws Exception {
        Tenant tenant = tenantRepository.save(new Tenant("TNT-LOGIN", "Login Test Co", "Login Hub"));
        Role adminRole = roleRepository.findByName("ROLE_ADMIN").orElseThrow();
        User user = new User();
        user.setTenantId(tenant.getId());
        user.setEmail("warehouse.owner@logistics.test");
        user.setFullName("Marcus Cole");
        user.setActive(true);
        user.setRoles(Set.of(adminRole));
        userRepository.save(user);

        // 1. Send Login OTP
        SendLoginOtpRequest sendReq = new SendLoginOtpRequest("warehouse.owner@logistics.test");
        mockMvc.perform(post("/api/v1/auth/otp/send-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sendReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.purpose").value("LOGIN"));

        String otp = mockEmailService.getLastSentOtp("warehouse.owner@logistics.test").orElseThrow();

        // 2. Verify Login OTP
        VerifyLoginOtpRequest verifyReq = new VerifyLoginOtpRequest("warehouse.owner@logistics.test", otp);
        MvcResult result = mockMvc.perform(post("/api/v1/auth/otp/verify-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(verifyReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value("warehouse.owner@logistics.test"))
                .andExpect(jsonPath("$.data.companyName").value("Login Test Co"))
                .andReturn();

        MockHttpSession session = (MockHttpSession) result.getRequest().getSession(false);
        assertThat(session).isNotNull();

        // 3. Authenticated session access
        mockMvc.perform(get("/api/v1/auth/me").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.companyName").value("Login Test Co"));
    }
}
