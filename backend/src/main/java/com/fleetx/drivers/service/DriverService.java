package com.fleetx.drivers.service;

import com.fleetx.drivers.dto.DriverCreateRequest;
import com.fleetx.drivers.dto.DriverResponseDto;
import com.fleetx.drivers.dto.DriverUpdateRequest;
import com.fleetx.drivers.entity.Driver;
import com.fleetx.drivers.entity.DriverAvailability;
import com.fleetx.drivers.entity.DriverPerformanceMetrics;
import com.fleetx.drivers.repository.DriverPerformanceMetricsRepository;
import com.fleetx.drivers.repository.DriverRepository;
import com.fleetx.shared.context.TenantSecurityContext;
import com.fleetx.shared.contract.AlertPublisher;
import com.fleetx.shared.contract.DriverLookupService;
import com.fleetx.shared.dto.DriverSummaryDto;
import com.fleetx.shared.dto.PageResponse;
import com.fleetx.shared.exception.DomainException;
import com.fleetx.shared.exception.ResourceNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class DriverService implements DriverLookupService {

    private final DriverRepository driverRepository;
    private final DriverPerformanceMetricsRepository metricsRepository;
    private final AlertPublisher alertPublisher;

    public DriverService(DriverRepository driverRepository,
                         DriverPerformanceMetricsRepository metricsRepository,
                         AlertPublisher alertPublisher) {
        this.driverRepository = driverRepository;
        this.metricsRepository = metricsRepository;
        this.alertPublisher = alertPublisher;
    }

    @Transactional(readOnly = true)
    public List<DriverResponseDto> findAllDrivers() {
        Long tenantId = TenantSecurityContext.getOptionalTenantId().orElse(1L);
        return driverRepository.findByTenantId(tenantId).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PageResponse<DriverResponseDto> searchDrivers(String search, String availability, Boolean active, Pageable pageable) {
        Long tenantId = TenantSecurityContext.getOptionalTenantId().orElse(1L);
        Page<Driver> page = driverRepository.findWithFilters(tenantId, search, availability, active, pageable);
        List<DriverResponseDto> content = page.getContent().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());

        return new PageResponse<>(
                content,
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isLast()
        );
    }

    @Transactional(readOnly = true)
    public DriverResponseDto getDriverById(Long id) {
        Long tenantId = TenantSecurityContext.getOptionalTenantId().orElse(1L);
        Driver driver = driverRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Driver", id));
        return mapToDto(driver);
    }

    @Transactional
    public DriverResponseDto createDriver(DriverCreateRequest request) {
        Long tenantId = TenantSecurityContext.getOptionalTenantId().orElse(1L);
        String cleanLicense = request.licenseNumber().trim().toUpperCase();
        if (driverRepository.existsByTenantIdAndLicenseNumber(tenantId, cleanLicense)) {
            throw new DomainException("A driver with license number '" + cleanLicense + "' already exists.",
                    HttpStatus.CONFLICT, "RESOURCE_CONFLICT");
        }

        if (request.licenseExpiryDate().isBefore(LocalDate.now())) {
            throw new DomainException("License expiry date cannot be in the past.",
                    HttpStatus.BAD_REQUEST, "VALIDATION_FAILED");
        }

        Driver driver = new Driver();
        driver.setTenantId(tenantId);
        driver.setUserId(request.userId());
        driver.setFullName(request.fullName().trim());
        driver.setPhoneNumber(request.phoneNumber().trim());
        driver.setEmail(request.email() != null ? request.email().trim().toLowerCase() : null);
        driver.setLicenseNumber(cleanLicense);
        driver.setLicenseCategory(request.licenseCategory() != null ? request.licenseCategory() : "HEAVY");
        driver.setLicenseExpiryDate(request.licenseExpiryDate());
        driver.setMedicalCertExpiryDate(request.medicalCertExpiryDate());
        driver.setExperienceYears(request.experienceYears() != null ? request.experienceYears() : 0);
        driver.setOperationalNotes(request.operationalNotes());
        driver.setActive(true);

        if (request.availabilityStatus() != null) {
            DriverAvailability avail = DriverAvailability.fromString(request.availabilityStatus());
            driver.setAvailabilityStatus(avail.name());
        } else {
            driver.setAvailabilityStatus(DriverAvailability.AVAILABLE.name());
        }

        Driver saved = driverRepository.save(driver);

        // Initialize driver performance metrics
        DriverPerformanceMetrics metrics = new DriverPerformanceMetrics(saved.getId());
        metrics.setTenantId(tenantId);
        metricsRepository.save(metrics);

        // Check if license is expiring soon (within 30 days) and trigger alert
        if (saved.getLicenseExpiryDate().isBefore(LocalDate.now().plusDays(30))) {
            alertPublisher.publishAlert(
                    "DRIVER_LICENSE_EXPIRING",
                    "WARNING",
                    "DRIVER",
                    saved.getId(),
                    "Driver " + saved.getFullName() + " license " + saved.getLicenseNumber() + " expires on " + saved.getLicenseExpiryDate(),
                    Instant.now()
            );
        }

        return mapToDto(saved);
    }

    @Transactional
    public DriverResponseDto updateDriver(Long id, DriverUpdateRequest request) {
        Long tenantId = TenantSecurityContext.getOptionalTenantId().orElse(1L);
        Driver driver = driverRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Driver", id));

        driver.setFullName(request.fullName().trim());
        driver.setPhoneNumber(request.phoneNumber().trim());
        driver.setEmail(request.email() != null ? request.email().trim().toLowerCase() : null);

        if (request.licenseCategory() != null) {
            driver.setLicenseCategory(request.licenseCategory());
        }
        if (request.licenseExpiryDate() != null) {
            driver.setLicenseExpiryDate(request.licenseExpiryDate());
        }
        if (request.medicalCertExpiryDate() != null) {
            driver.setMedicalCertExpiryDate(request.medicalCertExpiryDate());
        }
        if (request.experienceYears() != null) {
            driver.setExperienceYears(request.experienceYears());
        }
        if (request.availabilityStatus() != null) {
            DriverAvailability avail = DriverAvailability.fromString(request.availabilityStatus());
            driver.setAvailabilityStatus(avail.name());
        }
        if (request.active() != null) {
            driver.setActive(request.active());
        }
        if (request.operationalNotes() != null) {
            driver.setOperationalNotes(request.operationalNotes());
        }

        Driver saved = driverRepository.save(driver);
        return mapToDto(saved);
    }

    @Transactional
    public DriverResponseDto updateDriverAvailability(Long id, String status) {
        Long tenantId = TenantSecurityContext.getOptionalTenantId().orElse(1L);
        Driver driver = driverRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Driver", id));

        DriverAvailability avail = DriverAvailability.fromString(status);
        driver.setAvailabilityStatus(avail.name());
        Driver saved = driverRepository.save(driver);
        return mapToDto(saved);
    }

    // --- Contract Implementation: DriverLookupService ---

    @Override
    @Transactional(readOnly = true)
    public Optional<DriverSummaryDto> findDriverSummaryById(Long driverId) {
        Optional<Long> optionalTenantId = TenantSecurityContext.getOptionalTenantId();
        return driverRepository.findById(driverId)
                .filter(d -> optionalTenantId.isEmpty() || d.getTenantId().equals(optionalTenantId.get()))
                .map(d -> new DriverSummaryDto(
                        d.getId(),
                        d.getUserId(),
                        d.getLicenseNumber(),
                        d.getLicenseCategory(),
                        d.getAvailabilityStatus(),
                        d.getLicenseExpiryDate()
                ));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isDriverEligibleForTrip(Long driverId) {
        Optional<Long> optionalTenantId = TenantSecurityContext.getOptionalTenantId();
        return driverRepository.findById(driverId)
                .filter(d -> optionalTenantId.isEmpty() || d.getTenantId().equals(optionalTenantId.get()))
                .map(d -> Boolean.TRUE.equals(d.getActive())
                        && "AVAILABLE".equalsIgnoreCase(d.getAvailabilityStatus())
                        && d.getLicenseExpiryDate() != null
                        && d.getLicenseExpiryDate().isAfter(LocalDate.now())
                        && (d.getMedicalCertExpiryDate() == null || !d.getMedicalCertExpiryDate().isBefore(LocalDate.now())))
                .orElse(false);
    }

    @Override
    @Transactional
    public void setDriverAvailability(Long driverId, String status) {
        Optional<Long> optionalTenantId = TenantSecurityContext.getOptionalTenantId();
        driverRepository.findById(driverId).ifPresent(d -> {
            if (optionalTenantId.isEmpty() || d.getTenantId().equals(optionalTenantId.get())) {
                DriverAvailability avail = DriverAvailability.fromString(status);
                d.setAvailabilityStatus(avail.name());
                driverRepository.save(d);
            }
        });
    }

    private DriverResponseDto mapToDto(Driver d) {
        DriverPerformanceMetrics metrics = metricsRepository.findByDriverId(d.getId()).orElse(null);
        BigDecimal score = metrics != null ? metrics.getRatingScore() : new BigDecimal("100.00");
        Integer completed = metrics != null ? metrics.getTotalTripsCompleted() : 0;

        return new DriverResponseDto(
                d.getId(),
                d.getUserId(),
                d.getFullName(),
                d.getPhoneNumber(),
                d.getEmail(),
                d.getLicenseNumber(),
                d.getLicenseCategory(),
                d.getLicenseExpiryDate(),
                d.getMedicalCertExpiryDate(),
                d.getExperienceYears(),
                d.getAvailabilityStatus(),
                d.getActive(),
                d.getOperationalNotes(),
                d.getCreatedAt(),
                d.getUpdatedAt(),
                score,
                completed
        );
    }
}
