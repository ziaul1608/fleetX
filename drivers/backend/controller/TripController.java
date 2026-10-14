package com.fleetx.drivers.controller;

import com.fleetx.drivers.dto.TripCreateRequest;
import com.fleetx.drivers.dto.TripResponseDto;
import com.fleetx.drivers.dto.TripStatusHistoryResponseDto;
import com.fleetx.drivers.dto.TripStatusUpdateRequest;
import com.fleetx.drivers.service.TripService;
import com.fleetx.shared.dto.ApiResponse;
import com.fleetx.shared.dto.PageResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping({"/api/v1/trips", "/api/trips"})
public class TripController {

    private final TripService tripService;

    public TripController(TripService tripService) {
        this.tripService = tripService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<TripResponseDto>>> getTrips(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long driverId,
            @RequestParam(required = false) Long vehicleId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "desc") String direction
    ) {
        Sort sort = "asc".equalsIgnoreCase(direction) ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        PageResponse<TripResponseDto> trips = tripService.searchTrips(search, status, driverId, vehicleId, pageable);
        return ResponseEntity.ok(ApiResponse.ok("Trips retrieved successfully.", trips));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TripResponseDto>> getTripById(@PathVariable Long id) {
        TripResponseDto trip = tripService.getTripById(id);
        return ResponseEntity.ok(ApiResponse.ok("Trip details retrieved successfully.", trip));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'FLEET_MANAGER', 'DISPATCHER')")
    public ResponseEntity<ApiResponse<TripResponseDto>> createTrip(@Valid @RequestBody TripCreateRequest request) {
        TripResponseDto created = tripService.createTrip(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Trip created and assets assigned.", created));
    }

    @RequestMapping(value = "/{id}/status", method = {RequestMethod.PUT, RequestMethod.PATCH})
    @PreAuthorize("hasAnyRole('ADMIN', 'FLEET_MANAGER', 'DISPATCHER', 'DRIVER')")
    public ResponseEntity<ApiResponse<TripResponseDto>> updateTripStatus(
            @PathVariable Long id,
            @Valid @RequestBody TripStatusUpdateRequest request
    ) {
        TripResponseDto updated = tripService.updateTripStatus(id, request.targetStatus(), request.remarks(), null);
        return ResponseEntity.ok(ApiResponse.ok("Trip status updated successfully.", updated));
    }

    @PostMapping("/{id}/start")
    @PreAuthorize("hasAnyRole('ADMIN', 'FLEET_MANAGER', 'DISPATCHER', 'DRIVER')")
    public ResponseEntity<ApiResponse<TripResponseDto>> startTrip(
            @PathVariable Long id,
            @RequestParam(required = false, defaultValue = "Trip departed origin depot.") String remarks
    ) {
        TripResponseDto started = tripService.updateTripStatus(id, "IN_PROGRESS", remarks, null);
        return ResponseEntity.ok(ApiResponse.ok("Trip started successfully.", started));
    }

    @PostMapping("/{id}/complete")
    @PreAuthorize("hasAnyRole('ADMIN', 'FLEET_MANAGER', 'DISPATCHER', 'DRIVER')")
    public ResponseEntity<ApiResponse<TripResponseDto>> completeTrip(
            @PathVariable Long id,
            @RequestParam(required = false, defaultValue = "Trip delivered to destination.") String remarks
    ) {
        TripResponseDto completed = tripService.updateTripStatus(id, "COMPLETED", remarks, null);
        return ResponseEntity.ok(ApiResponse.ok("Trip completed successfully.", completed));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('ADMIN', 'FLEET_MANAGER', 'DISPATCHER')")
    public ResponseEntity<ApiResponse<TripResponseDto>> cancelTrip(
            @PathVariable Long id,
            @RequestParam(required = false, defaultValue = "Trip cancelled by dispatcher.") String remarks
    ) {
        TripResponseDto cancelled = tripService.updateTripStatus(id, "CANCELLED", remarks, null);
        return ResponseEntity.ok(ApiResponse.ok("Trip cancelled successfully.", cancelled));
    }

    @GetMapping("/{id}/history")
    public ResponseEntity<ApiResponse<List<TripStatusHistoryResponseDto>>> getTripHistory(@PathVariable Long id) {
        List<TripStatusHistoryResponseDto> history = tripService.getTripHistory(id);
        return ResponseEntity.ok(ApiResponse.ok("Trip status history retrieved successfully.", history));
    }
}
