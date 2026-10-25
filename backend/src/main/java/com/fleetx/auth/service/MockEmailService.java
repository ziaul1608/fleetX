package com.fleetx.auth.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Profile("test")
public class MockEmailService implements EmailService {

    private static final Logger log = LoggerFactory.getLogger(MockEmailService.class);

    private final ConcurrentHashMap<String, String> lastSentOtpByEmail = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, String> lastSentPurposeByEmail = new ConcurrentHashMap<>();

    public void recordSentOtp(String email, String otp, String purpose) {
        if (email != null) {
            String key = email.trim().toLowerCase();
            lastSentOtpByEmail.put(key, otp);
            lastSentPurposeByEmail.put(key, purpose);
        }
    }

    public Optional<String> getLastSentOtp(String email) {
        if (email == null) return Optional.empty();
        return Optional.ofNullable(lastSentOtpByEmail.get(email.trim().toLowerCase()));
    }

    public Optional<String> getLastSentPurpose(String email) {
        if (email == null) return Optional.empty();
        return Optional.ofNullable(lastSentPurposeByEmail.get(email.trim().toLowerCase()));
    }

    public void clear() {
        lastSentOtpByEmail.clear();
        lastSentPurposeByEmail.clear();
    }

    @Override
    public void sendOtp(String toEmail, String otpCode, String purpose, String recipientName) {
        recordSentOtp(toEmail, otpCode, purpose);
        log.info("[MOCK EMAIL] Captured OTP for {} (purpose: {})", toEmail, purpose);
    }
}
