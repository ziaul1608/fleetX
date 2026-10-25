package com.fleetx.auth;

import com.fleetx.auth.service.SmtpEmailService;
import com.fleetx.shared.exception.DomainException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class SmtpEmailServiceFailClosedTest {

    @Test
    @DisplayName("SmtpEmailService must fail closed with 503 SERVICE_UNAVAILABLE when JavaMailSender is unconfigured")
    void shouldFailClosedWhenMailSenderUnconfigured() {
        @SuppressWarnings("unchecked")
        ObjectProvider<JavaMailSender> emptyProvider = mock(ObjectProvider.class);
        when(emptyProvider.getIfAvailable()).thenReturn(null);

        SmtpEmailService smtpService = new SmtpEmailService(emptyProvider);
        ReflectionTestUtils.setField(smtpService, "mailFrom", "noreply@fleetx.test");

        assertThatThrownBy(() -> smtpService.sendOtp("owner@warehouse.test", "123456", "Login", "Owner"))
                .isInstanceOf(DomainException.class)
                .satisfies(ex -> {
                    DomainException de = (DomainException) ex;
                    assertThat(de.getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
                    assertThat(de.getErrorCode()).isEqualTo("EMAIL_SERVICE_UNAVAILABLE");
                    assertThat(de.getMessage()).contains("unconfigured or unavailable");
                });
    }

    @Test
    @DisplayName("SmtpEmailService must fail closed with 503 SERVICE_UNAVAILABLE when mail provider send fails")
    void shouldFailClosedWhenMailDispatchThrowsException() {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        doThrow(new MailSendException("SMTP connection refused"))
                .when(mailSender).send(any(SimpleMailMessage.class));

        @SuppressWarnings("unchecked")
        ObjectProvider<JavaMailSender> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(mailSender);

        SmtpEmailService smtpService = new SmtpEmailService(provider);
        ReflectionTestUtils.setField(smtpService, "mailFrom", "noreply@fleetx.test");

        assertThatThrownBy(() -> smtpService.sendOtp("owner@warehouse.test", "654321", "Signup", "Owner"))
                .isInstanceOf(DomainException.class)
                .satisfies(ex -> {
                    DomainException de = (DomainException) ex;
                    assertThat(de.getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
                    assertThat(de.getErrorCode()).isEqualTo("EMAIL_DISPATCH_FAILED");
                    assertThat(de.getMessage()).contains("Failed to dispatch verification code");
                });
    }

    @Test
    @DisplayName("SmtpEmailService dispatches email message cleanly when mail sender is available")
    void shouldDispatchCleanlyWhenMailSenderAvailable() {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        @SuppressWarnings("unchecked")
        ObjectProvider<JavaMailSender> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(mailSender);

        SmtpEmailService smtpService = new SmtpEmailService(provider);
        ReflectionTestUtils.setField(smtpService, "mailFrom", "noreply@fleetx.test");

        smtpService.sendOtp("owner@warehouse.test", "888999", "Login", "Alice Owner");

        verify(mailSender, times(1)).send(any(SimpleMailMessage.class));
    }
}
