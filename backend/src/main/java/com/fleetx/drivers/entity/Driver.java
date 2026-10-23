package com.fleetx.drivers.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "drivers")
public class Driver {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", unique = true)
    private Long userId;

    @Column(name = "full_name", nullable = false, length = 120)
    private String fullName = "Commercial Driver";

    @Column(name = "phone_number", nullable = false, length = 30)
    private String phoneNumber = "+91 90000 00000";

    @Column(name = "email", length = 180)
    private String email;

    @Column(name = "license_number", nullable = false, unique = true, length = 50)
    private String licenseNumber;

    @Column(name = "license_category", nullable = false, length = 30)
    private String licenseCategory = "HEAVY";

    @Column(name = "license_expiry_date", nullable = false)
    private LocalDate licenseExpiryDate;

    @Column(name = "medical_cert_expiry_date")
    private LocalDate medicalCertExpiryDate;

    @Column(name = "experience_years", nullable = false)
    private Integer experienceYears = 0;

    @Column(name = "availability_status", nullable = false, length = 30)
    private String availabilityStatus = "AVAILABLE";

    @Column(name = "active", nullable = false)
    private Boolean active = true;

    @Column(name = "operational_notes", columnDefinition = "TEXT")
    private String operationalNotes;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId = 1L;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public Driver() {}

    @PrePersist
    public void onPrePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        updatedAt = Instant.now();
        if (active == null) {
            active = true;
        }
        if (experienceYears == null) {
            experienceYears = 0;
        }
        if (availabilityStatus == null) {
            availabilityStatus = "AVAILABLE";
        }
    }

    @PreUpdate
    public void onPreUpdate() {
        updatedAt = Instant.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getLicenseNumber() { return licenseNumber; }
    public void setLicenseNumber(String licenseNumber) { this.licenseNumber = licenseNumber != null ? licenseNumber.trim().toUpperCase() : null; }

    public String getLicenseCategory() { return licenseCategory; }
    public void setLicenseCategory(String licenseCategory) { this.licenseCategory = licenseCategory != null ? licenseCategory.trim().toUpperCase() : "HEAVY"; }

    public LocalDate getLicenseExpiryDate() { return licenseExpiryDate; }
    public void setLicenseExpiryDate(LocalDate licenseExpiryDate) { this.licenseExpiryDate = licenseExpiryDate; }

    public LocalDate getMedicalCertExpiryDate() { return medicalCertExpiryDate; }
    public void setMedicalCertExpiryDate(LocalDate medicalCertExpiryDate) { this.medicalCertExpiryDate = medicalCertExpiryDate; }

    public Integer getExperienceYears() { return experienceYears; }
    public void setExperienceYears(Integer experienceYears) { this.experienceYears = experienceYears; }

    public String getAvailabilityStatus() { return availabilityStatus; }
    public void setAvailabilityStatus(String availabilityStatus) { this.availabilityStatus = availabilityStatus != null ? availabilityStatus.trim().toUpperCase() : "AVAILABLE"; }

    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }

    public String getOperationalNotes() { return operationalNotes; }
    public void setOperationalNotes(String operationalNotes) { this.operationalNotes = operationalNotes; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public Long getTenantId() { return tenantId; }
    public void setTenantId(Long tenantId) { this.tenantId = tenantId; }
}
