package com.fleetx.auth.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@Primary
public class SmtpEmailService implements EmailService {

    private static final Logger log = LoggerFactory.getLogger(SmtpEmailService.class);

    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final MockEmailService mockEmailService;

    @Value("${fleetx.mail.from:noreply@fleetx.local}")
    private String mailFrom;

    public SmtpEmailService(ObjectProvider<JavaMailSender> mailSenderProvider, MockEmailService mockEmailService) {
        this.mailSenderProvider = mailSenderProvider;
        this.mockEmailService = mockEmailService;
    }

    @Override
    public void sendOtp(String toEmail, String otpCode, String purpose, String recipientName) {
        // Always record in mock buffer for test assertion & inspection
        mockEmailService.recordSentOtp(toEmail, otpCode, purpose);

        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null) {
            log.info("[EMAIL DISPATCH] No SMTP server configured. Simulating OTP delivery to: {} (purpose: {})",
                    toEmail, purpose);
            return;
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
            log.info("[SMTP SUCCESS] Dispatched OTP verification email to: {}", toEmail);
        } catch (Exception ex) {
            log.warn("[SMTP ERROR] Could not deliver email to {}: {}. Captured in mock buffer for development/testing.",
                    toEmail, ex.getMessage());
        }
    }
}
