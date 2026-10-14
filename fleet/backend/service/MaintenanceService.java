package com.fleetx.fleet.service;

import com.fleetx.fleet.dto.MaintenanceRecordCreateRequest;
import com.fleetx.fleet.dto.MaintenanceRecordResponseDto;
import com.fleetx.fleet.dto.ServiceWarningResponseDto;
import com.fleetx.fleet.entity.*;
import com.fleetx.fleet.repository.MaintenanceRecordRepository;
import com.fleetx.fleet.repository.ServiceWarningRepository;
import com.fleetx.fleet.repository.VehicleRepository;
import com.fleetx.shared.contract.AlertPublisher;
import com.fleetx.shared.exception.DomainException;
import com.fleetx.shared.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class MaintenanceService {

    private static final Logger log = LoggerFactory.getLogger(MaintenanceService.class);
    private static final double UPCOMING_SERVICE_KM_THRESHOLD = 500.0;
    private static final long UPCOMING_SERVICE_DAYS_THRESHOLD = 14;

    private final MaintenanceRecordRepository maintenanceRepository;
    private final ServiceWarningRepository warningRepository;
    private final VehicleRepository vehicleRepository;
    private final AlertPublisher alertPublisher;

    public MaintenanceService(MaintenanceRecordRepository maintenanceRepository,
                              ServiceWarningRepository warningRepository,
                              VehicleRepository vehicleRepository,
                              AlertPublisher alertPublisher) {
        this.maintenanceRepository = maintenanceRepository;
        this.warningRepository = warningRepository;
        this.vehicleRepository = vehicleRepository;
        this.alertPublisher = alertPublisher;
    }

    @Transactional
    public MaintenanceRecordResponseDto recordMaintenance(Long vehicleId, MaintenanceRecordCreateRequest request) {
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle", vehicleId));

        // Validate service type
        ServiceType serviceType = ServiceType.fromString(request.getServiceType());

        // Validate dates and thresholds
        if (request.getNextServiceDueDate() != null && request.getNextServiceDueDate().isBefore(request.getServiceDate())) {
            throw new DomainException("Next service due date cannot be earlier than the current service date.",
                    org.springframework.http.HttpStatus.BAD_REQUEST, "INVALID_DATE_RANGE");
        }
        if (request.getNextServiceDueKm() != null && request.getNextServiceDueKm() <= request.getOdometerAtService()) {
            throw new DomainException("Next service due km must be strictly greater than odometer reading at service.",
                    org.springframework.http.HttpStatus.BAD_REQUEST, "INVALID_ODOMETER_RANGE");
        }

        MaintenanceRecord record = new MaintenanceRecord(
                vehicle,
                serviceType.name(),
                request.getDescription(),
                request.getCost(),
                request.getServiceDate(),
                request.getOdometerAtService(),
                request.getNextServiceDueKm(),
                request.getNextServiceDueDate(),
                request.getStatus()
        );

        // Update vehicle odometer if service odometer is higher
        if (request.getOdometerAtService() > vehicle.getOdometerKm()) {
            vehicle.setOdometerKm(request.getOdometerAtService());
        }

        // If maintenance is completed, resolve past unresolved warnings for this vehicle
        if ("COMPLETED".equalsIgnoreCase(record.getStatus())) {
            List<ServiceWarning> activeWarnings = warningRepository.findByVehicleIdAndResolvedFalse(vehicle.getId());
            for (ServiceWarning w : activeWarnings) {
                w.setResolved(true);
            }
            warningRepository.saveAll(activeWarnings);
        }

        MaintenanceRecord saved = maintenanceRepository.save(record);
        vehicleRepository.save(vehicle);

        // Re-evaluate warnings for any new thresholds
        checkAndPublishWarnings(vehicle);

        return mapToDto(saved);
    }

    @Transactional(readOnly = true)
    public Page<MaintenanceRecordResponseDto> getRecordsByVehicle(Long vehicleId, Pageable pageable) {
        if (!vehicleRepository.existsById(vehicleId)) {
            throw new ResourceNotFoundException("Vehicle", vehicleId);
        }
        return maintenanceRepository.findByVehicleIdOrderByServiceDateDesc(vehicleId, pageable)
                .map(this::mapToDto);
    }

    @Transactional(readOnly = true)
    public Page<MaintenanceRecordResponseDto> getAllRecords(Pageable pageable) {
        return maintenanceRepository.findAllByOrderByServiceDateDesc(pageable)
                .map(this::mapToDto);
    }

    @Transactional(readOnly = true)
    public List<ServiceWarningResponseDto> getActiveWarnings() {
        return warningRepository.findByResolvedFalseOrderByCreatedAtDesc().stream()
                .map(this::mapWarningToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public boolean hasServiceDue(Long vehicleId) {
        return !warningRepository.findByVehicleIdAndResolvedFalse(vehicleId).isEmpty();
    }

    @Transactional
    public void checkAndPublishWarnings(Vehicle vehicle) {
        LocalDate today = LocalDate.now();
        double currentOdometer = vehicle.getOdometerKm();

        // 1. Check km threshold from latest scheduled maintenance
        Optional<MaintenanceRecord> latestKmRecord =
                maintenanceRepository.findTopByVehicleIdAndNextServiceDueKmIsNotNullOrderByServiceDateDesc(vehicle.getId());

        if (latestKmRecord.isPresent()) {
            Double dueKm = latestKmRecord.get().getNextServiceDueKm();
            if (dueKm != null) {
                if (currentOdometer >= dueKm) {
                    double overdueBy = currentOdometer - dueKm;
                    String message = String.format("Vehicle %s is OVERDUE for maintenance by %.1f km (Current: %.1f km, Due: %.1f km).",
                            vehicle.getLicensePlate(), overdueBy, currentOdometer, dueKm);
                    createWarningAndPublishAlert(vehicle, WarningType.SERVICE_OVERDUE_KM.name(),
                            WarningSeverity.CRITICAL.name(), "SERVICE_OVERDUE", message);
                } else if ((dueKm - currentOdometer) <= UPCOMING_SERVICE_KM_THRESHOLD) {
                    double remaining = dueKm - currentOdometer;
                    String message = String.format("Vehicle %s maintenance is DUE SOON in %.1f km (Threshold: %.0f km).",
                            vehicle.getLicensePlate(), remaining, UPCOMING_SERVICE_KM_THRESHOLD);
                    createWarningAndPublishAlert(vehicle, WarningType.SERVICE_DUE_KM.name(),
                            WarningSeverity.WARNING.name(), "SERVICE_DUE", message);
                }
            }
        }

        // 2. Check date threshold from latest scheduled maintenance
        Optional<MaintenanceRecord> latestDateRecord =
                maintenanceRepository.findTopByVehicleIdAndNextServiceDueDateIsNotNullOrderByServiceDateDesc(vehicle.getId());

        if (latestDateRecord.isPresent()) {
            LocalDate dueDate = latestDateRecord.get().getNextServiceDueDate();
            if (dueDate != null) {
                if (today.isAfter(dueDate)) {
                    long daysOverdue = ChronoUnit.DAYS.between(dueDate, today);
                    String message = String.format("Vehicle %s is OVERDUE for service since %s (%d days overdue).",
                            vehicle.getLicensePlate(), dueDate, daysOverdue);
                    createWarningAndPublishAlert(vehicle, WarningType.SERVICE_OVERDUE_DATE.name(),
                            WarningSeverity.CRITICAL.name(), "SERVICE_OVERDUE", message);
                } else {
                    long daysRemaining = ChronoUnit.DAYS.between(today, dueDate);
                    if (daysRemaining <= UPCOMING_SERVICE_DAYS_THRESHOLD) {
                        String message = String.format("Vehicle %s service is due in %d days (on %s).",
                                vehicle.getLicensePlate(), daysRemaining, dueDate);
                        createWarningAndPublishAlert(vehicle, WarningType.SERVICE_DUE_DATE.name(),
                                WarningSeverity.WARNING.name(), "SERVICE_DUE", message);
                    }
                }
            }
        }
    }

    private void createWarningAndPublishAlert(Vehicle vehicle, String warningType, String severity,
                                              String alertType, String message) {
        boolean alreadyWarned = warningRepository.existsByVehicleIdAndWarningTypeAndResolvedFalse(
                vehicle.getId(), warningType);

        if (!alreadyWarned) {
            ServiceWarning warning = new ServiceWarning(vehicle, warningType, severity, message);
            warningRepository.save(warning);
            log.info("[Fleet Alert] Registered {} for vehicle #{} ({}): {}", severity, vehicle.getId(), vehicle.getLicensePlate(), message);

            // Publish alert through shared contract
            try {
                alertPublisher.publishAlert(
                        alertType,
                        severity,
                        "VEHICLE",
                        vehicle.getId(),
                        message,
                        Instant.now()
                );
            } catch (Exception ex) {
                log.warn("Failed to publish alert for vehicle {}: {}", vehicle.getId(), ex.getMessage());
            }
        }
    }

    private MaintenanceRecordResponseDto mapToDto(MaintenanceRecord r) {
        boolean isOverdue = false;
        if (r.getNextServiceDueKm() != null && r.getVehicle().getOdometerKm() >= r.getNextServiceDueKm()) {
            isOverdue = true;
        }
        if (r.getNextServiceDueDate() != null && LocalDate.now().isAfter(r.getNextServiceDueDate())) {
            isOverdue = true;
        }

        return new MaintenanceRecordResponseDto(
                r.getId(),
                r.getVehicle().getId(),
                r.getVehicle().getLicensePlate(),
                r.getServiceType(),
                r.getDescription(),
                r.getCost(),
                r.getServiceDate(),
                r.getOdometerAtService(),
                r.getNextServiceDueKm(),
                r.getNextServiceDueDate(),
                r.getStatus(),
                isOverdue,
                r.getCreatedAt()
        );
    }

    private ServiceWarningResponseDto mapWarningToDto(ServiceWarning w) {
        return new ServiceWarningResponseDto(
                w.getId(),
                w.getVehicle().getId(),
                w.getVehicle().getLicensePlate(),
                w.getWarningType(),
                w.getSeverity(),
                w.getMessage(),
                w.getResolved(),
                w.getCreatedAt()
        );
    }
}
