package com.fleetx.fleet.controller;

import com.fleetx.fleet.dto.*;
import com.fleetx.fleet.service.FuelService;
import com.fleetx.fleet.service.MaintenanceService;
import com.fleetx.fleet.service.VehicleService;
import com.fleetx.shared.dto.ApiResponse;
import com.fleetx.shared.dto.PageResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping({"/api/v1/fleet/vehicles", "/api/fleet/vehicles"})
public class VehicleController {

    private final VehicleService vehicleService;
    private final FuelService fuelService;
    private final MaintenanceService maintenanceService;

    public VehicleController(VehicleService vehicleService,
                             FuelService fuelService,
                             MaintenanceService maintenanceService) {
        this.vehicleService = vehicleService;
        this.fuelService = fuelService;
        this.maintenanceService = maintenanceService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<VehicleResponseDto>>> getVehicles(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id,desc") String[] sort) {

        Sort sortObj = Sort.by(Sort.Direction.DESC, "id");
        if (sort != null && sort.length > 0) {
            String property = sort[0];
            Sort.Direction direction = (sort.length > 1 && "asc".equalsIgnoreCase(sort[1]))
                    ? Sort.Direction.ASC : Sort.Direction.DESC;
            sortObj = Sort.by(direction, property);
        }

        Pageable pageable = PageRequest.of(page, size, sortObj);
        Page<VehicleResponseDto> vehiclePage = vehicleService.findVehicles(status, type, active, search, pageable);

        PageResponse<VehicleResponseDto> response = new PageResponse<>(
                vehiclePage.getContent(),
                vehiclePage.getNumber(),
                vehiclePage.getSize(),
                vehiclePage.getTotalElements(),
                vehiclePage.getTotalPages(),
                vehiclePage.isLast()
        );

        return ResponseEntity.ok(ApiResponse.ok("Vehicles retrieved successfully.", response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<VehicleResponseDto>> getVehicleById(@PathVariable Long id) {
        VehicleResponseDto vehicle = vehicleService.getVehicleById(id);
        return ResponseEntity.ok(ApiResponse.ok("Vehicle retrieved successfully.", vehicle));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'FLEET_MANAGER')")
    public ResponseEntity<ApiResponse<VehicleResponseDto>> createVehicle(
            @Valid @RequestBody VehicleCreateRequest request) {
        VehicleResponseDto created = vehicleService.createVehicle(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Vehicle registered successfully.", created));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'FLEET_MANAGER')")
    public ResponseEntity<ApiResponse<VehicleResponseDto>> updateVehicle(
            @PathVariable Long id,
            @Valid @RequestBody VehicleUpdateRequest request) {
        VehicleResponseDto updated = vehicleService.updateVehicle(id, request);
        return ResponseEntity.ok(ApiResponse.ok("Vehicle updated successfully.", updated));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'FLEET_MANAGER', 'DISPATCHER')")
    public ResponseEntity<ApiResponse<VehicleResponseDto>> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody VehicleStatusUpdateRequest request) {
        VehicleResponseDto updated = vehicleService.updateVehicleStatus(id, request);
        return ResponseEntity.ok(ApiResponse.ok("Vehicle status updated successfully.", updated));
    }

    @PatchMapping("/{id}/odometer")
    @PreAuthorize("hasAnyRole('ADMIN', 'FLEET_MANAGER', 'DRIVER')")
    public ResponseEntity<ApiResponse<VehicleResponseDto>> updateOdometer(
            @PathVariable Long id,
            @RequestParam Double newOdometerKm) {
        VehicleResponseDto updated = vehicleService.updateOdometer(id, newOdometerKm);
        return ResponseEntity.ok(ApiResponse.ok("Vehicle odometer updated successfully.", updated));
    }

    @PostMapping("/{id}/fuel-records")
    @PreAuthorize("hasAnyRole('ADMIN', 'FLEET_MANAGER', 'DRIVER')")
    public ResponseEntity<ApiResponse<FuelRecordResponseDto>> recordFueling(
            @PathVariable Long id,
            @Valid @RequestBody FuelRecordCreateRequest request) {
        FuelRecordResponseDto created = fuelService.recordFueling(id, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Fuel record logged successfully.", created));
    }

    @GetMapping("/{id}/fuel-records")
    public ResponseEntity<ApiResponse<PageResponse<FuelRecordResponseDto>>> getFuelRecords(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<FuelRecordResponseDto> recordsPage = fuelService.getRecordsByVehicle(id, pageable);

        PageResponse<FuelRecordResponseDto> response = new PageResponse<>(
                recordsPage.getContent(),
                recordsPage.getNumber(),
                recordsPage.getSize(),
                recordsPage.getTotalElements(),
                recordsPage.getTotalPages(),
                recordsPage.isLast()
        );

        return ResponseEntity.ok(ApiResponse.ok("Vehicle fuel records retrieved successfully.", response));
    }

    @PostMapping("/{id}/maintenance-records")
    @PreAuthorize("hasAnyRole('ADMIN', 'FLEET_MANAGER')")
    public ResponseEntity<ApiResponse<MaintenanceRecordResponseDto>> recordMaintenance(
            @PathVariable Long id,
            @Valid @RequestBody MaintenanceRecordCreateRequest request) {
        MaintenanceRecordResponseDto created = maintenanceService.recordMaintenance(id, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Maintenance record logged successfully.", created));
    }

    @GetMapping("/{id}/maintenance-records")
    public ResponseEntity<ApiResponse<PageResponse<MaintenanceRecordResponseDto>>> getMaintenanceRecords(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<MaintenanceRecordResponseDto> recordsPage = maintenanceService.getRecordsByVehicle(id, pageable);

        PageResponse<MaintenanceRecordResponseDto> response = new PageResponse<>(
                recordsPage.getContent(),
                recordsPage.getNumber(),
                recordsPage.getSize(),
                recordsPage.getTotalElements(),
                recordsPage.getTotalPages(),
                recordsPage.isLast()
        );

        return ResponseEntity.ok(ApiResponse.ok("Vehicle maintenance records retrieved successfully.", response));
    }
}
