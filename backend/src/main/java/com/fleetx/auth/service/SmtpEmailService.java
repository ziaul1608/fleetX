package com.fleetx.auth.service;

import com.fleetx.shared.exception.DomainException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@Profile("!test")
public class SmtpEmailService implements EmailService {

    private static final Logger log = LoggerFactory.getLogger(SmtpEmailService.class);

    private final ObjectProvider<JavaMailSender> mailSenderProvider;

    @Value("${fleetx.mail.from:noreply@fleetx.local}")
    private String mailFrom;

    public SmtpEmailService(ObjectProvider<JavaMailSender> mailSenderProvider) {
        this.mailSenderProvider = mailSenderProvider;
    }

    @Override
    public void sendOtp(String toEmail, String otpCode, String purpose, String recipientName) {
        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null) {
            log.error("[SMTP ERROR] Mail delivery service is unconfigured. Cannot dispatch verification code to: {}", toEmail);
            throw new DomainException(
                    "Email delivery service is currently unconfigured or unavailable. Please contact the system administrator.",
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "EMAIL_SERVICE_UNAVAILABLE"
            );
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(mailFrom);
            message.setTo(toEmail);
            message.setSubject("FleetX Verification Code: " + otpCode);
            message.setText("Hello " + (recipientName != null && !recipientName.isBlank() ? recipientName.trim() : "FleetX User") + ",\n\n"
                    + "Your FleetX " + purpose.toLowerCase() + " one-time verification code is:\n\n"
                    + "   " + otpCode + "\n\n"
                    + "This code is valid for 5 minutes. Do not share this code with anyone.\n\n"
                    + "Best regards,\n"
                    + "FleetX Intelligent Fleet & Logistics Management");
            mailSender.send(message);
            log.info("[SMTP SUCCESS] Dispatched OTP verification email to: {} (purpose: {})", toEmail, purpose);
        } catch (Exception ex) {
            log.error("[SMTP ERROR] Failed to deliver verification email to {}: {}", toEmail, ex.getMessage());
            throw new DomainException(
                    "Failed to dispatch verification code via email provider. Please try again later.",
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "EMAIL_DISPATCH_FAILED"
            );
        }
    }
}
