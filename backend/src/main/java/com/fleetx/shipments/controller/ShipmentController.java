package com.fleetx.shipments.controller;

import com.fleetx.shared.dto.ApiResponse;
import com.fleetx.shared.dto.PageResponse;
import com.fleetx.shipments.dto.*;
import com.fleetx.shipments.service.ShipmentService;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping({"/api/v1/shipments", "/api/shipments"})
public class ShipmentController {

    private final ShipmentService shipmentService;

    public ShipmentController(ShipmentService shipmentService) {
        this.shipmentService = shipmentService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<ShipmentResponseDto>>> getShipments(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long tripId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "desc") String direction
    ) {
        Sort sort = "asc".equalsIgnoreCase(direction) ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        PageResponse<ShipmentResponseDto> shipments = shipmentService.searchShipments(search, status, tripId, pageable);
        return ResponseEntity.ok(ApiResponse.ok("Shipments retrieved successfully.", shipments));
    }

    @GetMapping("/{idOrTracking}")
    public ResponseEntity<ApiResponse<ShipmentResponseDto>> getShipment(@PathVariable String idOrTracking) {
        ShipmentResponseDto shipment = shipmentService.getShipmentByIdOrTracking(idOrTracking);
        return ResponseEntity.ok(ApiResponse.ok("Shipment details retrieved successfully.", shipment));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'FLEET_MANAGER', 'DISPATCHER')")
    public ResponseEntity<ApiResponse<ShipmentResponseDto>> createShipment(
            @Valid @RequestBody ShipmentCreateRequest request
    ) {
        ShipmentResponseDto created = shipmentService.createShipment(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Shipment registered in manifest successfully.", created));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'FLEET_MANAGER', 'DISPATCHER')")
    public ResponseEntity<ApiResponse<ShipmentResponseDto>> updateShipment(
            @PathVariable Long id,
            @Valid @RequestBody ShipmentUpdateRequest request
    ) {
        ShipmentResponseDto updated = shipmentService.updateShipment(id, request);
        return ResponseEntity.ok(ApiResponse.ok("Shipment manifest updated successfully.", updated));
    }

    @RequestMapping(value = "/{id}/assign-trip", method = {RequestMethod.POST, RequestMethod.PATCH, RequestMethod.PUT})
    @PreAuthorize("hasAnyRole('ADMIN', 'FLEET_MANAGER', 'DISPATCHER')")
    public ResponseEntity<ApiResponse<ShipmentResponseDto>> assignTrip(
            @PathVariable Long id,
            @Valid @RequestBody ShipmentAssignTripRequest request
    ) {
        ShipmentResponseDto assigned = shipmentService.assignShipmentToTrip(id, request.tripId());
        return ResponseEntity.ok(ApiResponse.ok("Shipment associated with trip successfully.", assigned));
    }

    @RequestMapping(value = "/{id}/status", method = {RequestMethod.PUT, RequestMethod.PATCH})
    @PreAuthorize("hasAnyRole('ADMIN', 'FLEET_MANAGER', 'DISPATCHER', 'DRIVER')")
    public ResponseEntity<ApiResponse<ShipmentResponseDto>> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody ShipmentStatusUpdateRequest request
    ) {
        ShipmentResponseDto updated = shipmentService.updateShipmentStatus(
                id, request.targetStatus(), request.locationSummary(), request.remarks()
        );
        return ResponseEntity.ok(ApiResponse.ok("Shipment status updated successfully.", updated));
    }

    @PostMapping("/{id}/deliver")
    @PreAuthorize("hasAnyRole('ADMIN', 'FLEET_MANAGER', 'DISPATCHER', 'DRIVER')")
    public ResponseEntity<ApiResponse<ShipmentResponseDto>> completeDelivery(
            @PathVariable Long id,
            @RequestBody(required = false) ShipmentDeliverRequest request
    ) {
        String notes = request != null ? request.deliveryNotes() : null;
        String location = request != null ? request.locationSummary() : null;
        ShipmentResponseDto delivered = shipmentService.completeDelivery(id, notes, location);
        return ResponseEntity.ok(ApiResponse.ok("Shipment delivery completed successfully.", delivered));
    }

    @RequestMapping(value = "/{id}/waypoints", method = {RequestMethod.POST, RequestMethod.PUT})
    @PreAuthorize("hasAnyRole('ADMIN', 'FLEET_MANAGER', 'DISPATCHER')")
    public ResponseEntity<ApiResponse<RoutePlanResponseDto>> saveWaypoints(
            @PathVariable Long id,
            @Valid @RequestBody WaypointsUpdateRequest request
    ) {
        RoutePlanResponseDto routePlan = shipmentService.saveWaypointsAndCalculateRoute(id, request.waypoints());
        return ResponseEntity.ok(ApiResponse.ok("Route waypoints saved and Haversine estimate computed.", routePlan));
    }

    @GetMapping("/{id}/waypoints")
    public ResponseEntity<ApiResponse<RoutePlanResponseDto>> getWaypoints(@PathVariable Long id) {
        RoutePlanResponseDto routePlan = shipmentService.getRoutePlan(id);
        return ResponseEntity.ok(ApiResponse.ok("Route plan retrieved successfully.", routePlan));
    }

    @GetMapping("/{idOrTracking}/timeline")
    public ResponseEntity<ApiResponse<List<ShipmentTimelineEventDto>>> getTimeline(
            @PathVariable String idOrTracking
    ) {
        List<ShipmentTimelineEventDto> timeline = shipmentService.getShipmentTimeline(idOrTracking);
        return ResponseEntity.ok(ApiResponse.ok("Shipment delivery timeline retrieved successfully.", timeline));
    }

    @PostMapping("/check-delayed")
    @PreAuthorize("hasAnyRole('ADMIN', 'FLEET_MANAGER', 'DISPATCHER')")
    public ResponseEntity<ApiResponse<Map<String, Integer>>> checkDelayedShipments() {
        int flaggedCount = shipmentService.detectAndFlagDelayedShipments();
        return ResponseEntity.ok(ApiResponse.ok(
                "Delayed shipment inspection completed.",
                Map.of("flaggedDelayedCount", flaggedCount)
        ));
    }
}
