package com.fleetx.drivers.controller;

import com.fleetx.drivers.dto.*;
import com.fleetx.drivers.service.DriverPerformanceService;
import com.fleetx.drivers.service.DriverService;
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

@RestController
@RequestMapping({"/api/v1/trips/drivers", "/api/v1/drivers", "/api/trips/drivers", "/api/drivers"})
public class DriverController {

    private final DriverService driverService;
    private final DriverPerformanceService performanceService;

    public DriverController(DriverService driverService, DriverPerformanceService performanceService) {
        this.driverService = driverService;
        this.performanceService = performanceService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<DriverResponseDto>>> getDrivers(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String availability,
            @RequestParam(required = false) Boolean active,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "desc") String direction
    ) {
        Sort sort = "asc".equalsIgnoreCase(direction) ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        PageResponse<DriverResponseDto> drivers = driverService.searchDrivers(search, availability, active, pageable);
        return ResponseEntity.ok(ApiResponse.ok("Drivers retrieved successfully.", drivers));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<DriverResponseDto>> getDriverById(@PathVariable Long id) {
        DriverResponseDto driver = driverService.getDriverById(id);
        return ResponseEntity.ok(ApiResponse.ok("Driver profile retrieved successfully.", driver));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'FLEET_MANAGER', 'DISPATCHER')")
    public ResponseEntity<ApiResponse<DriverResponseDto>> createDriver(@Valid @RequestBody DriverCreateRequest request) {
        DriverResponseDto created = driverService.createDriver(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Driver onboarded successfully.", created));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'FLEET_MANAGER', 'DISPATCHER')")
    public ResponseEntity<ApiResponse<DriverResponseDto>> updateDriver(
            @PathVariable Long id,
            @Valid @RequestBody DriverUpdateRequest request
    ) {
        DriverResponseDto updated = driverService.updateDriver(id, request);
        return ResponseEntity.ok(ApiResponse.ok("Driver updated successfully.", updated));
    }

    @PatchMapping("/{id}/availability")
    @PreAuthorize("hasAnyRole('ADMIN', 'FLEET_MANAGER', 'DISPATCHER')")
    public ResponseEntity<ApiResponse<DriverResponseDto>> updateAvailability(
            @PathVariable Long id,
            @Valid @RequestBody DriverStatusUpdateRequest request
    ) {
        DriverResponseDto updated = driverService.updateDriverAvailability(id, request.availabilityStatus());
        return ResponseEntity.ok(ApiResponse.ok("Driver availability updated successfully.", updated));
    }

    @GetMapping("/{id}/performance")
    public ResponseEntity<ApiResponse<DriverPerformanceResponseDto>> getDriverPerformance(@PathVariable Long id) {
        DriverPerformanceResponseDto perf = performanceService.getPerformanceByDriverId(id);
        return ResponseEntity.ok(ApiResponse.ok("Driver performance retrieved successfully.", perf));
    }
}
