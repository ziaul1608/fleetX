package com.fleetx.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fleetx.auth.service.MockEmailService;
import com.fleetx.auth.service.ResendEmailService;
import com.fleetx.shared.exception.DomainException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

public class ResendEmailServiceTest {

    private ObjectMapper objectMapper;
    private RestClient.Builder restClientBuilder;
    private MockRestServiceServer mockServer;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        restClientBuilder = RestClient.builder();
        mockServer = MockRestServiceServer.bindTo(restClientBuilder).build();
    }

    @Test
    @DisplayName("Requirement 1 & 2: Correct API request composition and successful Resend 2xx response")
    void testSuccessfulEmailDispatch() {
        String apiKey = "re_live_test_api_key_12345";
        String from = "onboarding@resend.dev";
        String apiUrl = "https://api.resend.com/emails";
        String hmacSecret = "independent-high-entropy-otp-hmac-secret-32b";

        mockServer.expect(requestTo("https://api.resend.com/emails"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer " + apiKey))
                .andExpect(header("User-Agent", "FleetX-SaaS/1.0"))
                .andExpect(header("Content-Type", MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.from").value("onboarding@resend.dev"))
                .andExpect(jsonPath("$.to[0]").value("warehouse.owner@logistics.test"))
                .andExpect(jsonPath("$.subject").value("FleetX Verification Code: 654321"))
                .andExpect(jsonPath("$.text").value(containsString("654321")))
                .andRespond(withSuccess("{\"id\":\"msg_resend_98765\"}", MediaType.APPLICATION_JSON));

        ResendEmailService emailService = new ResendEmailService(
                apiKey, from, apiUrl, hmacSecret, objectMapper, restClientBuilder.build()
        );

        emailService.sendOtp("warehouse.owner@logistics.test", "654321", "Signup", "Vikram Singhania");

        mockServer.verify();
    }

    @Test
    @DisplayName("Requirement 3: Missing or placeholder API credentials fail closed with 503 SERVICE_UNAVAILABLE")
    void testMissingApiKeyFailsClosed() {
        String from = "onboarding@resend.dev";
        String apiUrl = "https://api.resend.com/emails";
        String hmacSecret = "independent-high-entropy-otp-hmac-secret-32b";

        // Case 1: Empty API key
        ResendEmailService emptyKeyService = new ResendEmailService(
                "", from, apiUrl, hmacSecret, objectMapper, restClientBuilder.build()
        );
        assertThatThrownBy(() -> emptyKeyService.sendOtp("owner@test.com", "123456", "Login", "Owner"))
                .isInstanceOf(DomainException.class)
                .satisfies(ex -> {
                    DomainException de = (DomainException) ex;
                    assertThat(de.getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
                    assertThat(de.getErrorCode()).isEqualTo("EMAIL_SERVICE_UNAVAILABLE");
                    assertThat(de.getMessage()).contains("unconfigured or unavailable");
                });

        // Case 2: Placeholder API key
        ResendEmailService placeholderService = new ResendEmailService(
                "your-resend-api-key-here", from, apiUrl, hmacSecret, objectMapper, restClientBuilder.build()
        );
        assertThatThrownBy(() -> placeholderService.sendOtp("owner@test.com", "123456", "Login", "Owner"))
                .isInstanceOf(DomainException.class)
                .satisfies(ex -> {
                    DomainException de = (DomainException) ex;
                    assertThat(de.getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
                    assertThat(de.getErrorCode()).isEqualTo("EMAIL_SERVICE_UNAVAILABLE");
                });
    }

    @Test
    @DisplayName("Requirement 4: Unverified domain or rejected provider request fails closed with 503")
    void testRejectedProviderRequestFailsClosed() {
        String apiKey = "re_live_test_api_key_12345";
        mockServer.expect(requestTo("https://api.resend.com/emails"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.UNPROCESSABLE_ENTITY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"statusCode\":422,\"message\":\"The from address does not belong to any verified domain.\",\"name\":\"validation_error\"}"));

        ResendEmailService emailService = new ResendEmailService(
                apiKey, "noreply@unverified-domain.com", "https://api.resend.com/emails",
                "secret-hmac-32b", objectMapper, restClientBuilder.build()
        );

        assertThatThrownBy(() -> emailService.sendOtp("owner@test.com", "123456", "Login", "Owner"))
                .isInstanceOf(DomainException.class)
                .satisfies(ex -> {
                    DomainException de = (DomainException) ex;
                    assertThat(de.getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
                    assertThat(de.getErrorCode()).isEqualTo("EMAIL_DISPATCH_FAILED");
                    assertThat(de.getMessage()).contains("verified domain");
                });

        mockServer.verify();
    }

    @Test
    @DisplayName("Requirement 5: Connection timeout or provider outage fails closed with 503")
    void testTimeoutFailsClosed() {
        mockServer.expect(requestTo("https://api.resend.com/emails"))
                .andRespond(withException(new java.net.SocketTimeoutException("Read timed out after 10000ms")));

        ResendEmailService timeoutService = new ResendEmailService(
                "re_test_key", "onboarding@resend.dev", "https://api.resend.com/emails",
                "secret-hmac-32b", objectMapper, restClientBuilder.build()
        );

        assertThatThrownBy(() -> timeoutService.sendOtp("owner@test.com", "123456", "Login", "Owner"))
                .isInstanceOf(DomainException.class)
                .satisfies(ex -> {
                    DomainException de = (DomainException) ex;
                    assertThat(de.getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
                    assertThat(de.getErrorCode()).isEqualTo("EMAIL_DISPATCH_FAILED");
                    assertThat(de.getMessage()).contains("timed out or was unreachable");
                });

        mockServer.verify();
    }

    @Test
    @DisplayName("Requirement 6: Rate-limited response (HTTP 429) and Server Error (HTTP 500) fail closed with 503")
    void testRateLimitAndServerErrorFailClosed() {
        String apiKey = "re_live_test_api_key_12345";

        // 6a. HTTP 429 Rate Limited
        mockServer.expect(requestTo("https://api.resend.com/emails"))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"statusCode\":429,\"message\":\"Too many requests\",\"name\":\"rate_limit_exceeded\"}"));

        ResendEmailService emailService = new ResendEmailService(
                apiKey, "onboarding@resend.dev", "https://api.resend.com/emails",
                "secret-hmac-32b", objectMapper, restClientBuilder.build()
        );

        assertThatThrownBy(() -> emailService.sendOtp("owner@test.com", "123456", "Login", "Owner"))
                .isInstanceOf(DomainException.class)
                .satisfies(ex -> {
                    DomainException de = (DomainException) ex;
                    assertThat(de.getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
                    assertThat(de.getErrorCode()).isEqualTo("EMAIL_DISPATCH_FAILED");
                    assertThat(de.getMessage()).contains("rate-limited");
                });

        mockServer.verify();
        mockServer.reset();

        // 6b. HTTP 500 Server Error
        mockServer.expect(requestTo("https://api.resend.com/emails"))
                .andRespond(withServerError());

        assertThatThrownBy(() -> emailService.sendOtp("owner@test.com", "123456", "Login", "Owner"))
                .isInstanceOf(DomainException.class)
                .satisfies(ex -> {
                    DomainException de = (DomainException) ex;
                    assertThat(de.getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
                    assertThat(de.getErrorCode()).isEqualTo("EMAIL_DISPATCH_FAILED");
                });

        mockServer.verify();
    }

    @Test
    @DisplayName("Requirement 7: Masking helper correctly redacts email addresses for logs")
    void testEmailMaskingForLogs() {
        assertThat(ResendEmailService.maskEmail("admin@fleetx.local")).isEqualTo("a***n@fleetx.local");
        assertThat(ResendEmailService.maskEmail("vikram.singhania@apex-logistics.in")).isEqualTo("v***a@apex-logistics.in");
        assertThat(ResendEmailService.maskEmail("ab@test.com")).isEqualTo("a***@test.com");
        assertThat(ResendEmailService.maskEmail(null)).isEqualTo("***");
        assertThat(ResendEmailService.maskEmail("invalid-email")).isEqualTo("***");
    }

    @Test
    @DisplayName("Requirement 8: HTTPS enforcement and HMAC-secret independence invariant")
    void testSecurityInvariants() {
        String apiKey = "re_live_secret_key_99999";

        // Non-HTTPS endpoint must be rejected
        ResendEmailService httpService = new ResendEmailService(
                apiKey, "onboarding@resend.dev", "http://insecure.api.resend.com/emails",
                "independent-hmac-key-32b", objectMapper, restClientBuilder.build()
        );
        assertThatThrownBy(() -> httpService.sendOtp("owner@test.com", "123456", "Login", "Owner"))
                .isInstanceOf(DomainException.class)
                .satisfies(ex -> {
                    DomainException de = (DomainException) ex;
                    assertThat(de.getErrorCode()).isEqualTo("INSECURE_EMAIL_ENDPOINT");
                });

        // HMAC secret equal to API key must be rejected
        ResendEmailService sameSecretService = new ResendEmailService(
                apiKey, "onboarding@resend.dev", "https://api.resend.com/emails",
                apiKey, objectMapper, restClientBuilder.build()
        );
        assertThatThrownBy(() -> sameSecretService.sendOtp("owner@test.com", "123456", "Login", "Owner"))
                .isInstanceOf(DomainException.class)
                .satisfies(ex -> {
                    DomainException de = (DomainException) ex;
                    assertThat(de.getErrorCode()).isEqualTo("INVALID_SECURITY_CONFIGURATION");
                });
    }

    @Test
    @DisplayName("Requirement 9: MockEmailService is strictly test-only and not active in production")
    void testMockEmailServiceIsTestOnly() {
        org.springframework.context.annotation.Profile profileAnnotation =
                MockEmailService.class.getAnnotation(org.springframework.context.annotation.Profile.class);

        assertThat(profileAnnotation).isNotNull();
        assertThat(profileAnnotation.value()).containsExactly("test");
    }

    @Test
    @DisplayName("Requirement 10: Provider rejection never results in false positive success")
    void testProviderFailureNeverTreatedAsSuccess() {
        mockServer.expect(requestTo("https://api.resend.com/emails"))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"statusCode\":400,\"message\":\"Validation error: invalid email format\"}"));

        ResendEmailService emailService = new ResendEmailService(
                "re_test_key", "onboarding@resend.dev", "https://api.resend.com/emails",
                "secret-hmac-32b", objectMapper, restClientBuilder.build()
        );

        assertThatThrownBy(() -> emailService.sendOtp("invalid@domain.invalid", "112233", "Login", "User"))
                .isInstanceOf(DomainException.class)
                .satisfies(ex -> {
                    DomainException de = (DomainException) ex;
                    assertThat(de.getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
                    assertThat(de.getErrorCode()).isEqualTo("EMAIL_DISPATCH_FAILED");
                });

        mockServer.verify();
    }
}

