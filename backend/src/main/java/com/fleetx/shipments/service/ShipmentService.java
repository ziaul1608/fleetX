package com.fleetx.shipments.service;

import com.fleetx.shared.context.TenantSecurityContext;
import com.fleetx.shared.contract.AlertPublisher;
import com.fleetx.shared.contract.ShipmentLookupService;
import com.fleetx.shared.contract.TripLookupService;
import com.fleetx.shared.dto.PageResponse;
import com.fleetx.shared.dto.ShipmentSummaryDto;
import com.fleetx.shared.dto.TripSummaryDto;
import com.fleetx.shared.exception.DomainException;
import com.fleetx.shared.exception.ResourceNotFoundException;
import com.fleetx.shipments.dto.*;
import com.fleetx.shipments.entity.*;
import com.fleetx.shipments.repository.ShipmentRepository;
import com.fleetx.shipments.repository.ShipmentTimelineEventRepository;
import com.fleetx.shipments.repository.ShipmentWaypointRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Service
public class ShipmentService implements ShipmentLookupService {

    private static final Logger log = LoggerFactory.getLogger(ShipmentService.class);

    private final ShipmentRepository shipmentRepository;
    private final ShipmentWaypointRepository waypointRepository;
    private final ShipmentTimelineEventRepository timelineRepository;
    private final RouteCalculationService routeCalculationService;
    private final TripLookupService tripLookupService;
    private final AlertPublisher alertPublisher;

    private final AtomicLong trackingSequence = new AtomicLong(340);

    public ShipmentService(ShipmentRepository shipmentRepository,
                           ShipmentWaypointRepository waypointRepository,
                           ShipmentTimelineEventRepository timelineRepository,
                           RouteCalculationService routeCalculationService,
                           TripLookupService tripLookupService,
                           AlertPublisher alertPublisher) {
        this.shipmentRepository = shipmentRepository;
        this.waypointRepository = waypointRepository;
        this.timelineRepository = timelineRepository;
        this.routeCalculationService = routeCalculationService;
        this.tripLookupService = tripLookupService;
        this.alertPublisher = alertPublisher;
    }

    @Transactional(readOnly = true)
    public PageResponse<ShipmentResponseDto> searchShipments(String search, String status, Long tripId, Pageable pageable) {
        Long tenantId = TenantSecurityContext.getOptionalTenantId().orElse(1L);
        Page<Shipment> page = shipmentRepository.findWithFilters(tenantId, search, status, tripId, pageable);
        List<ShipmentResponseDto> content = page.getContent().stream()
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
    public ShipmentResponseDto getShipmentByIdOrTracking(String idOrTracking) {
        Shipment shipment = resolveShipment(idOrTracking);
        return mapToDto(shipment);
    }

    @Transactional
    public ShipmentResponseDto createShipment(ShipmentCreateRequest request) {
        Long tenantId = TenantSecurityContext.getOptionalTenantId().orElse(1L);
        String trackingNum;
        if (request.trackingNumber() != null && !request.trackingNumber().isBlank()) {
            trackingNum = request.trackingNumber().trim().toUpperCase();
            if (shipmentRepository.existsByTenantIdAndTrackingNumber(tenantId, trackingNum)) {
                throw new DomainException("A shipment with tracking number '" + trackingNum + "' already exists.",
                        HttpStatus.CONFLICT, "RESOURCE_CONFLICT");
            }
        } else {
            trackingNum = generateUniqueTrackingNumber();
        }

        TripSummaryDto linkedTrip = null;
        if (request.tripId() != null) {
            linkedTrip = validateTripForAssociation(request.tripId());
        }

        Shipment shipment = new Shipment();
        shipment.setTenantId(tenantId);
        shipment.setTrackingNumber(trackingNum);
        shipment.setSenderName(request.senderName().trim());
        shipment.setSenderAddress(request.senderAddress().trim());
        shipment.setReceiverName(request.receiverName().trim());
        shipment.setReceiverAddress(request.receiverAddress().trim());
        shipment.setCargoDescription(request.cargoDescription());
        shipment.setWeightKg(request.weightKg());
        shipment.setVolumeM3(request.volumeM3());
        shipment.setPromisedDeliveryTime(request.promisedDeliveryTime());
        shipment.setTripId(request.tripId());
        shipment.setStatus(linkedTrip != null ? ShipmentStatus.ASSIGNED.name() : ShipmentStatus.CREATED.name());

        Shipment saved = shipmentRepository.save(shipment);

        // Record creation timeline milestone
        ShipmentTimelineEvent event = new ShipmentTimelineEvent(
                saved.getId(),
                saved.getStatus(),
                saved.getSenderAddress(),
                linkedTrip != null
                        ? "Shipment created and assigned to Trip " + linkedTrip.tripCode()
                        : "Shipment manifest registered and awaiting dispatch."
        );
        event.setTenantId(tenantId);
        timelineRepository.save(event);

        return mapToDto(saved);
    }

    @Transactional
    public ShipmentResponseDto updateShipment(Long id, ShipmentUpdateRequest request) {
        Shipment shipment = resolveShipment(String.valueOf(id));

        shipment.setSenderName(request.senderName().trim());
        shipment.setSenderAddress(request.senderAddress().trim());
        shipment.setReceiverName(request.receiverName().trim());
        shipment.setReceiverAddress(request.receiverAddress().trim());
        shipment.setCargoDescription(request.cargoDescription());
        shipment.setWeightKg(request.weightKg());
        shipment.setVolumeM3(request.volumeM3());
        shipment.setPromisedDeliveryTime(request.promisedDeliveryTime());

        Shipment saved = shipmentRepository.save(shipment);
        checkAndAlertIfDelayed(saved);

        return mapToDto(saved);
    }

    @Transactional
    public ShipmentResponseDto assignShipmentToTrip(Long shipmentId, Long tripId) {
        Shipment shipment = resolveShipment(String.valueOf(shipmentId));

        ShipmentStatus currentStatus = ShipmentStatus.fromString(shipment.getStatus());
        if (currentStatus.isTerminal()) {
            throw new DomainException("Cannot assign a " + currentStatus + " shipment to a trip.",
                    HttpStatus.BAD_REQUEST, "INVALID_STATE_TRANSITION");
        }

        TripSummaryDto trip = validateTripForAssociation(tripId);
        shipment.setTripId(tripId);

        if (currentStatus == ShipmentStatus.CREATED) {
            shipment.setStatus(ShipmentStatus.ASSIGNED.name());
        }

        Shipment saved = shipmentRepository.save(shipment);

        ShipmentTimelineEvent event = new ShipmentTimelineEvent(
                saved.getId(),
                "ASSIGNED",
                saved.getSenderAddress(),
                "Associated with Trip " + trip.tripCode() + " (" + trip.originAddress() + " → " + trip.destinationAddress() + ")"
        );
        event.setTenantId(saved.getTenantId());
        timelineRepository.save(event);

        alertPublisher.publishAlert(
                "SHIPMENT_ASSIGNED",
                "INFO",
                "SHIPMENT",
                saved.getId(),
                "Shipment " + saved.getTrackingNumber() + " linked to trip " + trip.tripCode(),
                Instant.now()
        );

        return mapToDto(saved);
    }

    @Transactional
    public ShipmentResponseDto updateShipmentStatus(Long id, String targetStatusStr, String locationSummary, String remarks) {
        Shipment shipment = resolveShipment(String.valueOf(id));

        ShipmentStatus currentStatus = ShipmentStatus.fromString(shipment.getStatus());
        ShipmentStatus targetStatus;
        try {
            targetStatus = ShipmentStatus.fromString(targetStatusStr);
        } catch (IllegalArgumentException ex) {
            throw new DomainException("Invalid shipment status '" + targetStatusStr + "'.",
                    HttpStatus.BAD_REQUEST, "INVALID_SHIPMENT_STATUS");
        }

        if (!currentStatus.canTransitionTo(targetStatus)) {
            throw new DomainException("Illegal shipment status transition from " + currentStatus + " to " + targetStatus + ".",
                    HttpStatus.BAD_REQUEST, "INVALID_STATE_TRANSITION");
        }

        Instant now = Instant.now();
        shipment.setStatus(targetStatus.name());

        if (targetStatus == ShipmentStatus.DELIVERED) {
            shipment.setDeliveredAt(now);
            if (remarks != null && !remarks.isBlank()) {
                shipment.setDeliveryNotes(remarks.trim());
            }
            alertPublisher.publishAlert(
                    "SHIPMENT_DELIVERED",
                    "INFO",
                    "SHIPMENT",
                    shipment.getId(),
                    "Shipment " + shipment.getTrackingNumber() + " delivered to " + shipment.getReceiverName(),
                    now
            );
        } else if (targetStatus == ShipmentStatus.DELAYED) {
            alertPublisher.publishAlert(
                    "SHIPMENT_DELAYED",
                    "WARNING",
                    "SHIPMENT",
                    shipment.getId(),
                    "Shipment " + shipment.getTrackingNumber() + " flagged as DELAYED: " + (remarks != null ? remarks : "Behind schedule"),
                    now
            );
        } else if (targetStatus == ShipmentStatus.CANCELLED) {
            alertPublisher.publishAlert(
                    "SHIPMENT_CANCELLED",
                    "WARNING",
                    "SHIPMENT",
                    shipment.getId(),
                    "Shipment " + shipment.getTrackingNumber() + " cancelled: " + (remarks != null ? remarks : "Order cancelled"),
                    now
            );
        }

        Shipment saved = shipmentRepository.save(shipment);

        String loc = (locationSummary != null && !locationSummary.isBlank())
                ? locationSummary.trim()
                : (targetStatus == ShipmentStatus.DELIVERED ? saved.getReceiverAddress() : saved.getSenderAddress());

        ShipmentTimelineEvent event = new ShipmentTimelineEvent(
                saved.getId(),
                targetStatus.name(),
                loc,
                remarks != null && !remarks.isBlank() ? remarks.trim() : "Shipment transitioned to " + targetStatus.name()
        );
        event.setTenantId(saved.getTenantId());
        timelineRepository.save(event);

        return mapToDto(saved);
    }

    @Transactional
    public ShipmentResponseDto completeDelivery(Long id, String deliveryNotes, String locationSummary) {
        Shipment shipment = resolveShipment(String.valueOf(id));

        String loc = (locationSummary != null && !locationSummary.isBlank())
                ? locationSummary.trim()
                : shipment.getReceiverAddress();
        String notes = (deliveryNotes != null && !deliveryNotes.isBlank())
                ? deliveryNotes.trim()
                : "Consignment received and signed off at destination.";

        return updateShipmentStatus(shipment.getId(), ShipmentStatus.DELIVERED.name(), loc, notes);
    }

    @Transactional
    public RoutePlanResponseDto saveWaypointsAndCalculateRoute(Long shipmentId, List<WaypointItemRequest> waypointsReq) {
        Shipment shipment = resolveShipment(String.valueOf(shipmentId));

        List<WaypointItemRequest> sorted = routeCalculationService.validateAndSortWaypoints(waypointsReq);

        // Clear existing waypoints
        waypointRepository.deleteByShipmentId(shipmentId);
        waypointRepository.flush();

        Instant baseTime = Instant.now();
        double cumulativeDistanceKm = 0.0;
        double avgSpeed = routeCalculationService.getAverageSpeedKmh();
        int stopAllowance = routeCalculationService.getStopAllowanceMinutes();

        List<ShipmentWaypoint> savedWaypoints = new ArrayList<>();

        for (int i = 0; i < sorted.size(); i++) {
            WaypointItemRequest item = sorted.get(i);
            if (i > 0) {
                WaypointItemRequest prev = sorted.get(i - 1);
                cumulativeDistanceKm += routeCalculationService.calculateHaversineKm(
                        prev.latitude(), prev.longitude(), item.latitude(), item.longitude()
                );
            }

            long travelMinutes = Math.round((cumulativeDistanceKm / avgSpeed) * 60.0);
            int priorIntermediateStops = Math.max(0, i - 1);
            long totalMinutesToStop = travelMinutes + ((long) priorIntermediateStops * stopAllowance);
            Instant stopEta = baseTime.plus(totalMinutesToStop, ChronoUnit.MINUTES);

            String stopType = item.stopType();
            if (stopType == null || stopType.isBlank()) {
                stopType = (i == 0) ? StopType.ORIGIN.name()
                        : (i == sorted.size() - 1) ? StopType.DESTINATION.name()
                        : StopType.INTERMEDIATE.name();
            } else {
                stopType = StopType.fromString(stopType).name();
            }

            ShipmentWaypoint wp = new ShipmentWaypoint(
                    shipmentId,
                    item.waypointName().trim(),
                    BigDecimal.valueOf(item.latitude()).setScale(7, RoundingMode.HALF_UP),
                    BigDecimal.valueOf(item.longitude()).setScale(7, RoundingMode.HALF_UP),
                    i + 1,
                    stopType,
                    stopEta
            );
            wp.setTenantId(shipment.getTenantId());
            savedWaypoints.add(waypointRepository.save(wp));
        }

        BigDecimal totalDistanceBd = BigDecimal.valueOf(cumulativeDistanceKm).setScale(2, RoundingMode.HALF_UP);
        int totalDurationMinutes = routeCalculationService.estimateDurationMinutes(totalDistanceBd, sorted.size());
        Instant finalEta = baseTime.plus(totalDurationMinutes, ChronoUnit.MINUTES);

        shipment.setEstimatedDistanceKm(totalDistanceBd);
        shipment.setEstimatedDurationMinutes(totalDurationMinutes);
        shipment.setEstimatedArrivalTime(finalEta);
        Shipment updatedShipment = shipmentRepository.save(shipment);

        ShipmentTimelineEvent event = new ShipmentTimelineEvent(
                shipmentId,
                "ROUTE_PLANNED",
                sorted.get(0).waypointName() + " → " + sorted.get(sorted.size() - 1).waypointName(),
                String.format("Planned %d waypoints. Est. Straight-Line Distance: %s km, Est. Duration: %d mins.",
                        sorted.size(), totalDistanceBd, totalDurationMinutes)
        );
        event.setTenantId(shipment.getTenantId());
        timelineRepository.save(event);

        checkAndAlertIfDelayed(updatedShipment);

        List<WaypointResponseDto> wpDtos = savedWaypoints.stream()
                .map(this::mapWaypointToDto)
                .collect(Collectors.toList());

        return new RoutePlanResponseDto(
                shipmentId,
                updatedShipment.getTrackingNumber(),
                totalDistanceBd,
                totalDurationMinutes,
                finalEta,
                RouteCalculationService.DISTANCE_ESTIMATE_LABEL,
                RouteCalculationService.ETA_ESTIMATE_LABEL,
                avgSpeed,
                stopAllowance,
                wpDtos
        );
    }

    @Transactional(readOnly = true)
    public RoutePlanResponseDto getRoutePlan(Long shipmentId) {
        Shipment shipment = resolveShipment(String.valueOf(shipmentId));

        List<WaypointResponseDto> waypoints = waypointRepository.findByShipmentIdOrderBySequenceOrderAsc(shipment.getId())
                .stream()
                .map(this::mapWaypointToDto)
                .collect(Collectors.toList());

        return new RoutePlanResponseDto(
                shipment.getId(),
                shipment.getTrackingNumber(),
                shipment.getEstimatedDistanceKm() != null ? shipment.getEstimatedDistanceKm() : BigDecimal.ZERO,
                shipment.getEstimatedDurationMinutes() != null ? shipment.getEstimatedDurationMinutes() : 0,
                shipment.getEstimatedArrivalTime(),
                RouteCalculationService.DISTANCE_ESTIMATE_LABEL,
                RouteCalculationService.ETA_ESTIMATE_LABEL,
                routeCalculationService.getAverageSpeedKmh(),
                routeCalculationService.getStopAllowanceMinutes(),
                waypoints
        );
    }

    @Transactional(readOnly = true)
    public List<ShipmentTimelineEventDto> getShipmentTimeline(String idOrTracking) {
        Shipment shipment = resolveShipment(idOrTracking);
        return timelineRepository.findByShipmentIdOrderByCreatedAtAsc(shipment.getId()).stream()
                .map(e -> new ShipmentTimelineEventDto(
                        e.getId(),
                        e.getShipmentId(),
                        e.getEventType(),
                        e.getLocationSummary(),
                        e.getRemarks(),
                        e.getCreatedAt()
                ))
                .collect(Collectors.toList());
    }

    @Transactional
    public int detectAndFlagDelayedShipments() {
        Long tenantId = TenantSecurityContext.getOptionalTenantId().orElse(1L);
        Instant now = Instant.now();
        List<Shipment> overdue = shipmentRepository.findOverdueShipmentsByTenant(tenantId, now);
        for (Shipment s : overdue) {
            s.setStatus(ShipmentStatus.DELAYED.name());
            shipmentRepository.save(s);

            ShipmentTimelineEvent event = new ShipmentTimelineEvent(
                    s.getId(),
                    "DELAYED",
                    s.getReceiverAddress(),
                    "Promised delivery window (" + s.getPromisedDeliveryTime() + ") exceeded."
            );
            event.setTenantId(tenantId);
            timelineRepository.save(event);

            alertPublisher.publishAlert(
                    "SHIPMENT_DELAYED",
                    "WARNING",
                    "SHIPMENT",
                    s.getId(),
                    "Shipment " + s.getTrackingNumber() + " exceeded promised delivery time (" + s.getPromisedDeliveryTime() + ").",
                    now
            );
        }
        return overdue.size();
    }

    // --- Contract Implementation: ShipmentLookupService ---

    @Override
    @Transactional(readOnly = true)
    public Optional<ShipmentSummaryDto> findByTrackingNumber(String trackingNumber) {
        return shipmentRepository.findByTrackingNumber(trackingNumber)
                .map(s -> new ShipmentSummaryDto(
                        s.getId(),
                        s.getTrackingNumber(),
                        s.getTripId(),
                        s.getWeightKg(),
                        s.getStatus()
                ));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ShipmentSummaryDto> findShipmentsByTripId(Long tripId) {
        return shipmentRepository.findByTripId(tripId).stream()
                .map(s -> new ShipmentSummaryDto(
                        s.getId(),
                        s.getTrackingNumber(),
                        s.getTripId(),
                        s.getWeightKg(),
                        s.getStatus()
                ))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void recordTripMilestoneForShipments(Long tripId, String locationSummary, String eventType) {
        List<Shipment> shipments = shipmentRepository.findByTripId(tripId);
        for (Shipment s : shipments) {
            timelineRepository.save(new ShipmentTimelineEvent(
                    s.getId(),
                    eventType,
                    locationSummary,
                    "Automated trip telemetry milestone for Trip #" + tripId
            ));
        }
        log.info("Recorded milestone '{}' at location '{}' for {} shipments on trip #{}",
                eventType, locationSummary, shipments.size(), tripId);
    }

    // --- Internal Helpers ---

    private TripSummaryDto validateTripForAssociation(Long tripId) {
        Optional<TripSummaryDto> tripOpt = tripLookupService.findTripSummaryById(tripId);
        if (tripOpt.isEmpty()) {
            throw new DomainException("Trip #" + tripId + " does not exist.",
                    HttpStatus.BAD_REQUEST, "INVALID_TRIP_ASSOCIATION");
        }
        TripSummaryDto trip = tripOpt.get();
        if ("COMPLETED".equalsIgnoreCase(trip.status()) || "CANCELLED".equalsIgnoreCase(trip.status())) {
            throw new DomainException("Cannot associate shipment with a " + trip.status() + " trip (" + trip.tripCode() + ").",
                    HttpStatus.BAD_REQUEST, "INVALID_TRIP_ASSOCIATION");
        }
        return trip;
    }

    private void checkAndAlertIfDelayed(Shipment s) {
        if (s.getPromisedDeliveryTime() == null) {
            return;
        }
        ShipmentStatus status = ShipmentStatus.fromString(s.getStatus());
        if (status.isTerminal()) {
            return;
        }

        Instant now = Instant.now();
        boolean pastPromised = now.isAfter(s.getPromisedDeliveryTime());
        boolean etaPastPromised = s.getEstimatedArrivalTime() != null
                && s.getEstimatedArrivalTime().isAfter(s.getPromisedDeliveryTime());

        if (pastPromised || etaPastPromised) {
            if (status != ShipmentStatus.DELAYED) {
                s.setStatus(ShipmentStatus.DELAYED.name());
                shipmentRepository.save(s);

                timelineRepository.save(new ShipmentTimelineEvent(
                        s.getId(),
                        "DELAYED",
                        s.getSenderAddress(),
                        etaPastPromised
                                ? "Estimated arrival (" + s.getEstimatedArrivalTime() + ") exceeds promised delivery time (" + s.getPromisedDeliveryTime() + ")."
                                : "Promised delivery time (" + s.getPromisedDeliveryTime() + ") has passed."
                ));
            }

            alertPublisher.publishAlert(
                    "SHIPMENT_DELAYED",
                    "WARNING",
                    "SHIPMENT",
                    s.getId(),
                    "Shipment " + s.getTrackingNumber() + " is projected or running late against promised delivery time " + s.getPromisedDeliveryTime(),
                    now
            );
        }
    }

    private Shipment resolveShipment(String idOrTracking) {
        if (idOrTracking == null || idOrTracking.isBlank()) {
            throw new ResourceNotFoundException("Shipment identifier cannot be blank.");
        }
        Long tenantId = TenantSecurityContext.getOptionalTenantId().orElse(1L);
        String clean = idOrTracking.trim();
        if (clean.matches("^\\d+$")) {
            Long id = Long.parseLong(clean);
            Optional<Shipment> byId = shipmentRepository.findByIdAndTenantId(id, tenantId);
            if (byId.isPresent()) {
                return byId.get();
            }
        }
        return shipmentRepository.findByTrackingNumberAndTenantId(clean.toUpperCase(), tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Shipment with identifier '" + idOrTracking + "' not found."));
    }

    private String generateUniqueTrackingNumber() {
        int year = LocalDate.now().getYear();
        String code;
        do {
            long seq = trackingSequence.incrementAndGet();
            code = String.format("SHP-%d-%04d", year, seq);
        } while (shipmentRepository.existsByTrackingNumber(code));
        return code;
    }

    private WaypointResponseDto mapWaypointToDto(ShipmentWaypoint wp) {
        return new WaypointResponseDto(
                wp.getId(),
                wp.getShipmentId(),
                wp.getWaypointName(),
                wp.getLatitude(),
                wp.getLongitude(),
                wp.getSequenceOrder(),
                wp.getStopType(),
                wp.getEstimatedArrival()
        );
    }

    private ShipmentResponseDto mapToDto(Shipment s) {
        String tripCode = null;
        if (s.getTripId() != null) {
            tripCode = tripLookupService.findTripSummaryById(s.getTripId())
                    .map(TripSummaryDto::tripCode)
                    .orElse("TRIP-#" + s.getTripId());
        }

        boolean isLate = false;
        if (s.getPromisedDeliveryTime() != null) {
            if (s.getDeliveredAt() != null) {
                isLate = s.getDeliveredAt().isAfter(s.getPromisedDeliveryTime());
            } else if (!"CANCELLED".equalsIgnoreCase(s.getStatus())) {
                isLate = Instant.now().isAfter(s.getPromisedDeliveryTime())
                        || (s.getEstimatedArrivalTime() != null && s.getEstimatedArrivalTime().isAfter(s.getPromisedDeliveryTime()))
                        || "DELAYED".equalsIgnoreCase(s.getStatus());
            }
        } else if ("DELAYED".equalsIgnoreCase(s.getStatus())) {
            isLate = true;
        }

        return new ShipmentResponseDto(
                s.getId(),
                s.getTrackingNumber(),
                s.getTripId(),
                tripCode,
                s.getSenderName(),
                s.getSenderAddress(),
                s.getReceiverName(),
                s.getReceiverAddress(),
                s.getCargoDescription(),
                s.getWeightKg(),
                s.getVolumeM3(),
                s.getStatus(),
                s.getPromisedDeliveryTime(),
                s.getEstimatedDistanceKm(),
                s.getEstimatedDurationMinutes(),
                s.getEstimatedArrivalTime(),
                s.getDeliveredAt(),
                s.getDeliveryNotes(),
                isLate,
                s.getCreatedAt(),
                s.getUpdatedAt()
        );
    }
}
