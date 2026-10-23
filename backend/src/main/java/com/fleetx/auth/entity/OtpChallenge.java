package com.fleetx.auth.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "otp_challenges")
public class OtpChallenge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 180)
    private String email;

    @Column(name = "otp_hash", nullable = false)
    private String otpHash;

    @Column(nullable = false, length = 30)
    private String purpose;

    @Column(nullable = false)
    private int attempts = 0;

    @Column(name = "max_attempts", nullable = false)
    private int maxAttempts = 5;

    @Column(name = "resend_available_at", nullable = false)
    private Instant resendAvailableAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(nullable = false)
    private boolean verified = false;

    @Column(name = "verified_at")
    private Instant verifiedAt;

    @Column(name = "metadata_json", columnDefinition = "TEXT")
    private String metadataJson;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public OtpChallenge() {}

    public OtpChallenge(String email, String otpHash, String purpose, Instant resendAvailableAt, Instant expiresAt, String metadataJson, String ipAddress) {
        this.email = email != null ? email.trim().toLowerCase() : null;
        this.otpHash = otpHash;
        this.purpose = purpose;
        this.resendAvailableAt = resendAvailableAt;
        this.expiresAt = expiresAt;
        this.metadataJson = metadataJson;
        this.ipAddress = ipAddress;
        this.attempts = 0;
        this.maxAttempts = 5;
        this.verified = false;
        this.createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email != null ? email.trim().toLowerCase() : null; }

    public String getOtpHash() { return otpHash; }
    public void setOtpHash(String otpHash) { this.otpHash = otpHash; }

    public String getPurpose() { return purpose; }
    public void setPurpose(String purpose) { this.purpose = purpose; }

    public int getAttempts() { return attempts; }
    public void setAttempts(int attempts) { this.attempts = attempts; }

    public int getMaxAttempts() { return maxAttempts; }
    public void setMaxAttempts(int maxAttempts) { this.maxAttempts = maxAttempts; }

    public Instant getResendAvailableAt() { return resendAvailableAt; }
    public void setResendAvailableAt(Instant resendAvailableAt) { this.resendAvailableAt = resendAvailableAt; }

    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }

    public boolean isVerified() { return verified; }
    public void setVerified(boolean verified) { this.verified = verified; }

    public Instant getVerifiedAt() { return verifiedAt; }
    public void setVerifiedAt(Instant verifiedAt) { this.verifiedAt = verifiedAt; }

    public String getMetadataJson() { return metadataJson; }
    public void setMetadataJson(String metadataJson) { this.metadataJson = metadataJson; }

    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
