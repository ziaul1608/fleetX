package com.fleetx.drivers.service;

import com.fleetx.drivers.dto.TripCreateRequest;
import com.fleetx.drivers.dto.TripResponseDto;
import com.fleetx.drivers.dto.TripStatusHistoryResponseDto;
import com.fleetx.drivers.entity.Driver;
import com.fleetx.drivers.entity.Trip;
import com.fleetx.drivers.entity.TripStatus;
import com.fleetx.drivers.entity.TripStatusHistory;
import com.fleetx.drivers.repository.DriverRepository;
import com.fleetx.drivers.repository.TripRepository;
import com.fleetx.drivers.repository.TripStatusHistoryRepository;
import com.fleetx.shared.context.TenantSecurityContext;
import com.fleetx.shared.contract.AlertPublisher;
import com.fleetx.shared.contract.TripLookupService;
import com.fleetx.shared.contract.VehicleLookupService;
import com.fleetx.shared.dto.PageResponse;
import com.fleetx.shared.dto.TripSummaryDto;
import com.fleetx.shared.dto.VehicleSummaryDto;
import com.fleetx.shared.exception.DomainException;
import com.fleetx.shared.exception.ResourceNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Service
public class TripService implements TripLookupService {

    private final TripRepository tripRepository;
    private final TripStatusHistoryRepository historyRepository;
    private final DriverRepository driverRepository;
    private final DriverService driverService;
    private final DriverPerformanceService driverPerformanceService;
    private final VehicleLookupService vehicleLookupService;
    private final AlertPublisher alertPublisher;

    private final AtomicLong codeSequence = new AtomicLong(100);

    public TripService(TripRepository tripRepository,
                       TripStatusHistoryRepository historyRepository,
                       DriverRepository driverRepository,
                       DriverService driverService,
                       DriverPerformanceService driverPerformanceService,
                       VehicleLookupService vehicleLookupService,
                       AlertPublisher alertPublisher) {
        this.tripRepository = tripRepository;
        this.historyRepository = historyRepository;
        this.driverRepository = driverRepository;
        this.driverService = driverService;
        this.driverPerformanceService = driverPerformanceService;
        this.vehicleLookupService = vehicleLookupService;
        this.alertPublisher = alertPublisher;
    }

    @Transactional(readOnly = true)
    public PageResponse<TripResponseDto> searchTrips(String search, String status, Long driverId, Long vehicleId, Pageable pageable) {
        Long tenantId = TenantSecurityContext.requireTenantId();
        Page<Trip> page = tripRepository.findWithFilters(tenantId, search, status, driverId, vehicleId, pageable);
        List<TripResponseDto> content = page.getContent().stream()
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
    public TripResponseDto getTripById(Long id) {
        Long tenantId = TenantSecurityContext.requireTenantId();
        Trip trip = tripRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Trip", id));
        return mapToDto(trip);
    }

    @Transactional(readOnly = true)
    public List<TripStatusHistoryResponseDto> getTripHistory(Long tripId) {
        Long tenantId = TenantSecurityContext.requireTenantId();
        if (!tripRepository.existsByIdAndTenantId(tripId, tenantId)) {
            throw new ResourceNotFoundException("Trip", tripId);
        }
        return historyRepository.findByTripIdOrderByCreatedAtDesc(tripId).stream()
                .map(h -> new TripStatusHistoryResponseDto(
                        h.getId(),
                        h.getTripId(),
                        h.getFromStatus(),
                        h.getToStatus(),
                        h.getRemarks(),
                        h.getUpdatedBy(),
                        h.getCreatedAt()
                ))
                .collect(Collectors.toList());
    }

    @Transactional
    public TripResponseDto createTrip(TripCreateRequest request) {
        Long tenantId = TenantSecurityContext.requireTenantId();

        // Validate vehicle availability via shared VehicleLookupService
        boolean vehicleAvailable = vehicleLookupService.isVehicleAvailableForTrip(request.vehicleId());
        if (!vehicleAvailable) {
            throw new DomainException("Vehicle #" + request.vehicleId() + " is not available for trip assignment.",
                    HttpStatus.BAD_REQUEST, "VEHICLE_NOT_AVAILABLE");
        }

        // Validate driver eligibility via DriverLookupService
        boolean driverEligible = driverService.isDriverEligibleForTrip(request.driverId());
        if (!driverEligible) {
            throw new DomainException("Driver #" + request.driverId() + " is not eligible for trip assignment (unavailable, suspended, or expired license).",
                    HttpStatus.BAD_REQUEST, "DRIVER_NOT_ELIGIBLE");
        }

        if (request.scheduledEndTime() != null && request.scheduledEndTime().isBefore(request.scheduledStartTime())) {
            throw new DomainException("Scheduled end time cannot be before scheduled start time.",
                    HttpStatus.BAD_REQUEST, "INVALID_SCHEDULE_TIME");
        }

        Trip trip = new Trip();
        trip.setTenantId(tenantId);
        trip.setTripCode(generateUniqueTripCode());
        trip.setVehicleId(request.vehicleId());
        trip.setDriverId(request.driverId());
        trip.setOriginAddress(request.originAddress().trim());
        trip.setDestinationAddress(request.destinationAddress().trim());
        trip.setScheduledStartTime(request.scheduledStartTime());
        trip.setScheduledEndTime(request.scheduledEndTime());
        trip.setDistanceKm(request.distanceKm());
        trip.setNotes(request.notes());
        trip.setStatus(TripStatus.ASSIGNED.name());

        Trip saved = tripRepository.save(trip);

        // Update driver availability to ON_DUTY
        driverService.setDriverAvailability(request.driverId(), "ON_DUTY");

        // Update vehicle operational status to IN_TRIP
        vehicleLookupService.updateVehicleOperationalStatus(request.vehicleId(), "IN_TRIP");

        // Record status history
        TripStatusHistory history = new TripStatusHistory(
                saved.getId(),
                null,
                saved.getStatus(),
                "Trip created and assets allocated.",
                null
        );
        history.setTenantId(tenantId);
        historyRepository.save(history);

        // Publish alert
        alertPublisher.publishAlert(
                "TRIP_ASSIGNED",
                "INFO",
                "TRIP",
                saved.getId(),
                "Trip " + saved.getTripCode() + " assigned to vehicle #" + saved.getVehicleId() + " and driver #" + saved.getDriverId(),
                Instant.now()
        );

        return mapToDto(saved);
    }

    @Transactional
    public TripResponseDto updateTripStatus(Long tripId, String targetStatusStr, String remarks, Long updatedBy) {
        Long tenantId = TenantSecurityContext.requireTenantId();
        Trip trip = tripRepository.findByIdAndTenantId(tripId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Trip", tripId));

        TripStatus currentStatus = TripStatus.fromString(trip.getStatus());
        TripStatus targetStatus = TripStatus.fromString(targetStatusStr);

        if (currentStatus.isTerminal()) {
            throw new DomainException("Cannot change status of a " + currentStatus + " trip.",
                    HttpStatus.BAD_REQUEST, "INVALID_STATE_TRANSITION");
        }

        if (currentStatus == targetStatus) {
            return mapToDto(trip);
        }

        // Validate lifecycle transition rules:
        // PLANNED -> ASSIGNED or CANCELLED
        // ASSIGNED -> IN_PROGRESS or CANCELLED
        // IN_PROGRESS -> COMPLETED or CANCELLED
        if (targetStatus == TripStatus.COMPLETED && currentStatus != TripStatus.IN_PROGRESS) {
            throw new DomainException("Cannot complete a trip that has not started yet. Current status: " + currentStatus,
                    HttpStatus.BAD_REQUEST, "INVALID_STATE_TRANSITION");
        }

        boolean validTransition = switch (currentStatus) {
            case PLANNED -> targetStatus == TripStatus.ASSIGNED || targetStatus == TripStatus.CANCELLED;
            case ASSIGNED -> targetStatus == TripStatus.IN_PROGRESS || targetStatus == TripStatus.CANCELLED;
            case IN_PROGRESS -> targetStatus == TripStatus.COMPLETED || targetStatus == TripStatus.CANCELLED;
            default -> false;
        };

        if (!validTransition) {
            throw new DomainException("Invalid state transition from " + currentStatus + " to " + targetStatus + ".",
                    HttpStatus.BAD_REQUEST, "INVALID_STATE_TRANSITION");
        }

        Instant now = Instant.now();

        if (targetStatus == TripStatus.IN_PROGRESS) {
            trip.setActualStartTime(now);
            trip.setStatus(TripStatus.IN_PROGRESS.name());
            alertPublisher.publishAlert(
                    "TRIP_STARTED",
                    "INFO",
                    "TRIP",
                    trip.getId(),
                    "Trip " + trip.getTripCode() + " departed for " + trip.getDestinationAddress(),
                    now
            );
        } else if (targetStatus == TripStatus.COMPLETED) {
            trip.setActualEndTime(now);
            trip.setStatus(TripStatus.COMPLETED.name());

            // Release assets
            driverService.setDriverAvailability(trip.getDriverId(), "AVAILABLE");
            vehicleLookupService.updateVehicleOperationalStatus(trip.getVehicleId(), "AVAILABLE");

            // Update driver performance metrics
            boolean onTime = trip.getScheduledEndTime() == null || !now.isAfter(trip.getScheduledEndTime());
            driverPerformanceService.recordTripCompleted(trip.getDriverId(), onTime, trip.getDistanceKm());

            alertPublisher.publishAlert(
                    "TRIP_COMPLETED",
                    "INFO",
                    "TRIP",
                    trip.getId(),
                    "Trip " + trip.getTripCode() + " safely completed at destination.",
                    now
            );
        } else if (targetStatus == TripStatus.CANCELLED) {
            trip.setStatus(TripStatus.CANCELLED.name());

            // Release assets
            driverService.setDriverAvailability(trip.getDriverId(), "AVAILABLE");
            vehicleLookupService.updateVehicleOperationalStatus(trip.getVehicleId(), "AVAILABLE");

            // Update cancelled metric
            driverPerformanceService.recordTripCancelled(trip.getDriverId());

            alertPublisher.publishAlert(
                    "TRIP_CANCELLED",
                    "WARNING",
                    "TRIP",
                    trip.getId(),
                    "Trip " + trip.getTripCode() + " cancelled: " + (remarks != null ? remarks : "Operational cancellation"),
                    now
            );
        }

        Trip updated = tripRepository.save(trip);

        // Record history
        TripStatusHistory history = new TripStatusHistory(
                updated.getId(),
                currentStatus.name(),
                targetStatus.name(),
                remarks != null ? remarks : "Status changed to " + targetStatus,
                updatedBy
        );
        history.setTenantId(tenantId);
        historyRepository.save(history);

        return mapToDto(updated);
    }

    // --- Contract Implementation: TripLookupService ---

    @Override
    @Transactional(readOnly = true)
    public Optional<TripSummaryDto> findTripSummaryById(Long tripId) {
        Optional<Long> optionalTenantId = TenantSecurityContext.getOptionalTenantId();
        return tripRepository.findById(tripId)
                .filter(t -> optionalTenantId.isEmpty() || t.getTenantId().equals(optionalTenantId.get()))
                .map(t -> new TripSummaryDto(
                        t.getId(),
                        t.getTripCode(),
                        t.getVehicleId(),
                        t.getDriverId(),
                        t.getStatus(),
                        t.getOriginAddress(),
                        t.getDestinationAddress()
                ));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isTripActive(Long tripId) {
        Optional<Long> optionalTenantId = TenantSecurityContext.getOptionalTenantId();
        return tripRepository.findById(tripId)
                .filter(t -> optionalTenantId.isEmpty() || t.getTenantId().equals(optionalTenantId.get()))
                .map(t -> TripStatus.IN_PROGRESS.name().equalsIgnoreCase(t.getStatus())
                        || "IN_TRANSIT".equalsIgnoreCase(t.getStatus()))
                .orElse(false);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasActiveTripForVehicle(Long vehicleId) {
        if (vehicleId == null) {
            return false;
        }
        return tripRepository.existsByVehicleIdAndStatusIn(
                vehicleId,
                List.of(TripStatus.ASSIGNED.name(), TripStatus.IN_PROGRESS.name(), "IN_TRANSIT")
        );
    }

    private String generateUniqueTripCode() {
        int year = LocalDate.now().getYear();
        String code;
        do {
            long seq = codeSequence.incrementAndGet();
            code = String.format("TRIP-%d-%04d", year, seq);
        } while (tripRepository.existsByTripCode(code));
        return code;
    }

    private TripResponseDto mapToDto(Trip t) {
        String vehiclePlate = "N/A";
        String vehicleMakeModel = "Commercial Vehicle";
        Optional<VehicleSummaryDto> vSummary = vehicleLookupService.findVehicleSummaryById(t.getVehicleId());
        if (vSummary.isPresent()) {
            vehiclePlate = vSummary.get().licensePlate();
            vehicleMakeModel = vSummary.get().make() + " " + vSummary.get().model();
        }

        String driverName = "Assigned Driver";
        String driverPhone = "";
        Optional<Driver> dOpt = driverRepository.findById(t.getDriverId());
        if (dOpt.isPresent()) {
            driverName = dOpt.get().getFullName();
            driverPhone = dOpt.get().getPhoneNumber();
        }

        return new TripResponseDto(
                t.getId(),
                t.getTripCode(),
                t.getVehicleId(),
                t.getDriverId(),
                t.getOriginAddress(),
                t.getDestinationAddress(),
                t.getStatus(),
                t.getScheduledStartTime(),
                t.getScheduledEndTime(),
                t.getActualStartTime(),
                t.getActualEndTime(),
                t.getDistanceKm(),
                t.getNotes(),
                t.getCreatedAt(),
                t.getUpdatedAt(),
                vehiclePlate,
                vehicleMakeModel,
                driverName,
                driverPhone
        );
    }
}
