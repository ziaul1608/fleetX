package com.fleetx.auth.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fleetx.auth.dto.OtpSentResponse;
import com.fleetx.auth.dto.SendLoginOtpRequest;
import com.fleetx.auth.dto.SendSignupOtpRequest;
import com.fleetx.auth.entity.OtpChallenge;
import com.fleetx.auth.repository.OtpChallengeRepository;
import com.fleetx.shared.exception.DomainException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Deque;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

@Service
public class OtpService {

    private static final Logger log = LoggerFactory.getLogger(OtpService.class);

    public static final int VALIDITY_SECONDS = 300; // 5 minutes
    public static final int COOLDOWN_SECONDS = 60;  // 1 minute
    public static final int MAX_ATTEMPTS = 5;

    private static final int MAX_IP_REQUESTS_PER_WINDOW = 15;
    private static final Duration IP_WINDOW = Duration.ofMinutes(10);
    private static final int MAX_EMAIL_REQUESTS_PER_HOUR = 5;
    private static final Duration EMAIL_WINDOW = Duration.ofHours(1);

    private final OtpChallengeRepository otpChallengeRepository;
    private final EmailService emailService;
    private final ObjectMapper objectMapper;
    private final SecureRandom secureRandom = new SecureRandom();

    private final ConcurrentHashMap<String, Deque<Instant>> ipRequestTimestamps = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Deque<Instant>> emailRequestTimestamps = new ConcurrentHashMap<>();

    @Value("${fleetx.auth.otp.hmac-secret:fleetx-default-dev-hmac-secret-key-change-in-prod-32bytes}")
    private String hmacSecret;

    public OtpService(OtpChallengeRepository otpChallengeRepository, EmailService emailService, ObjectMapper objectMapper) {
        this.otpChallengeRepository = otpChallengeRepository;
        this.emailService = emailService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public OtpSentResponse sendSignupOtp(SendSignupOtpRequest request, String ipAddress) {
        String normalizedEmail = request.email().trim().toLowerCase();
        enforceCooldown(normalizedEmail, "SIGNUP");
        checkEmailHourlyLimit(normalizedEmail);
        checkIpRateLimit(ipAddress);

        String otp = generateNumericOtp();
        String hash = computeHmacHex(normalizedEmail, otp);

        String metadataJson;
        try {
            metadataJson = objectMapper.writeValueAsString(Map.of(
                    "companyName", request.companyName().trim(),
                    "warehouseName", request.warehouseName().trim(),
                    "fullName", request.fullName().trim()
            ));
        } catch (Exception e) {
            metadataJson = "{}";
        }

        Instant now = Instant.now();
        Instant resendAvailableAt = now.plusSeconds(COOLDOWN_SECONDS);
        Instant expiresAt = now.plusSeconds(VALIDITY_SECONDS);

        OtpChallenge challenge = new OtpChallenge(
                normalizedEmail,
                hash,
                "SIGNUP",
                resendAvailableAt,
                expiresAt,
                metadataJson,
                ipAddress
        );
        otpChallengeRepository.save(challenge);

        emailService.sendOtp(normalizedEmail, otp, "Signup", request.fullName().trim());
        log.info("[OTP GENERATED] Created SIGNUP challenge for: {}", normalizedEmail);

        return new OtpSentResponse(
                normalizedEmail,
                "SIGNUP",
                VALIDITY_SECONDS,
                COOLDOWN_SECONDS,
                "Verification code dispatched to " + normalizedEmail
        );
    }

    @Transactional
    public OtpSentResponse sendLoginOtp(SendLoginOtpRequest request, String ipAddress, String userFullName) {
        String normalizedEmail = request.email().trim().toLowerCase();
        enforceCooldown(normalizedEmail, "LOGIN");
        checkEmailHourlyLimit(normalizedEmail);
        checkIpRateLimit(ipAddress);

        String otp = generateNumericOtp();
        String hash = computeHmacHex(normalizedEmail, otp);

        Instant now = Instant.now();
        Instant resendAvailableAt = now.plusSeconds(COOLDOWN_SECONDS);
        Instant expiresAt = now.plusSeconds(VALIDITY_SECONDS);

        OtpChallenge challenge = new OtpChallenge(
                normalizedEmail,
                hash,
                "LOGIN",
                resendAvailableAt,
                expiresAt,
                "{}",
                ipAddress
        );
        otpChallengeRepository.save(challenge);

        emailService.sendOtp(normalizedEmail, otp, "Login", userFullName);
        log.info("[OTP GENERATED] Created LOGIN challenge for: {}", normalizedEmail);

        return new OtpSentResponse(
                normalizedEmail,
                "LOGIN",
                VALIDITY_SECONDS,
                COOLDOWN_SECONDS,
                "If an account is associated with this email address, a verification code has been dispatched."
        );
    }

    @Transactional
    public void verifyOtp(String email, String purpose, String rawOtp) {
        if (email == null || rawOtp == null) {
            throw new DomainException("Email and verification code are required.", HttpStatus.BAD_REQUEST, "MISSING_FIELDS");
        }

        String normalizedEmail = email.trim().toLowerCase();
        String cleanedOtp = rawOtp.trim();

        if (!cleanedOtp.matches("^[0-9]{6}$")) {
            throw new DomainException("Verification code must be exactly 6 numeric digits.", HttpStatus.BAD_REQUEST, "INVALID_OTP_FORMAT");
        }

        Optional<OtpChallenge> challengeOpt = otpChallengeRepository.findFirstByEmailAndPurposeOrderByIdDesc(normalizedEmail, purpose);
        if (challengeOpt.isEmpty()) {
            throw new DomainException("No active verification code found for this email. Please request a new code.",
                    HttpStatus.BAD_REQUEST, "NO_ACTIVE_OTP");
        }

        OtpChallenge challenge = challengeOpt.get();

        if (challenge.isVerified()) {
            throw new DomainException("This verification code has already been consumed. Please request a new code.",
                    HttpStatus.BAD_REQUEST, "OTP_ALREADY_USED");
        }

        Instant now = Instant.now();
        if (now.isAfter(challenge.getExpiresAt())) {
            throw new DomainException("Verification code has expired. Please request a new code.",
                    HttpStatus.BAD_REQUEST, "OTP_EXPIRED");
        }

        if (challenge.getAttempts() >= challenge.getMaxAttempts()) {
            throw new DomainException("Maximum verification attempts exceeded. Please request a new code.",
                    HttpStatus.TOO_MANY_REQUESTS, "MAX_ATTEMPTS_EXCEEDED");
        }

        challenge.setAttempts(challenge.getAttempts() + 1);

        String candidateHmac = computeHmacHex(normalizedEmail, cleanedOtp);
        if (!MessageDigest.isEqual(candidateHmac.getBytes(StandardCharsets.UTF_8), challenge.getOtpHash().getBytes(StandardCharsets.UTF_8))) {
            otpChallengeRepository.save(challenge);
            int remaining = Math.max(0, challenge.getMaxAttempts() - challenge.getAttempts());
            throw new DomainException("Incorrect verification code. Attempts remaining: " + remaining,
                    HttpStatus.BAD_REQUEST, "INCORRECT_OTP");
        }

        challenge.setVerified(true);
        challenge.setVerifiedAt(now);
        otpChallengeRepository.save(challenge);
        log.info("[OTP VERIFIED] Successfully verified {} challenge for: {}", purpose, normalizedEmail);
    }

    private void enforceCooldown(String email, String purpose) {
        Optional<OtpChallenge> existingOpt = otpChallengeRepository.findTopByEmailAndPurposeOrderByIdDesc(email, purpose);
        if (existingOpt.isPresent()) {
            OtpChallenge existing = existingOpt.get();
            Instant now = Instant.now();
            if (now.isBefore(existing.getResendAvailableAt()) && !existing.isVerified()) {
                long remainingSeconds = Duration.between(now, existing.getResendAvailableAt()).getSeconds() + 1;
                throw new DomainException(
                        "Please wait " + remainingSeconds + " seconds before requesting another verification code.",
                        HttpStatus.TOO_MANY_REQUESTS,
                        "OTP_COOLDOWN"
                );
            }
        }
    }

    private void checkIpRateLimit(String ipAddress) {
        if (ipAddress == null || ipAddress.isBlank()) return;
        Instant now = Instant.now();
        Instant windowStart = now.minus(IP_WINDOW);
        Deque<Instant> timestamps = ipRequestTimestamps.computeIfAbsent(ipAddress, k -> new ConcurrentLinkedDeque<>());
        synchronized (timestamps) {
            while (!timestamps.isEmpty() && timestamps.peekFirst().isBefore(windowStart)) {
                timestamps.pollFirst();
            }
            if (timestamps.size() >= MAX_IP_REQUESTS_PER_WINDOW) {
                throw new DomainException(
                        "Too many verification requests from this network. Please wait before trying again.",
                        HttpStatus.TOO_MANY_REQUESTS,
                        "IP_RATE_LIMIT_EXCEEDED"
                );
            }
            timestamps.addLast(now);
        }
    }

    private void checkEmailHourlyLimit(String email) {
        Instant now = Instant.now();
        Instant windowStart = now.minus(EMAIL_WINDOW);
        Deque<Instant> timestamps = emailRequestTimestamps.computeIfAbsent(email, k -> new ConcurrentLinkedDeque<>());
        synchronized (timestamps) {
            while (!timestamps.isEmpty() && timestamps.peekFirst().isBefore(windowStart)) {
                timestamps.pollFirst();
            }
            if (timestamps.size() >= MAX_EMAIL_REQUESTS_PER_HOUR) {
                throw new DomainException(
                        "Too many verification requests for this email address. Please try again later.",
                        HttpStatus.TOO_MANY_REQUESTS,
                        "EMAIL_RATE_LIMIT_EXCEEDED"
                );
            }
            timestamps.addLast(now);
        }
    }

    private String generateNumericOtp() {
        return String.format("%06d", secureRandom.nextInt(1_000_000));
    }

    public String computeHmacHex(String email, String rawOtp) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(hmacSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKey);
            String payload = email.toLowerCase().trim() + ":" + rawOtp.trim();
            byte[] hmacBytes = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(hmacBytes.length * 2);
            for (byte b : hmacBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to calculate HMAC-SHA-256 for verification challenge", e);
        }
    }

    public static String sha256Hex(String raw) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(raw.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 message digest unavailable", e);
        }
    }
}
