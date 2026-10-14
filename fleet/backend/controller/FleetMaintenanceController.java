package com.fleetx.fleet.controller;

import com.fleetx.fleet.dto.MaintenanceRecordResponseDto;
import com.fleetx.fleet.dto.ServiceWarningResponseDto;
import com.fleetx.fleet.service.MaintenanceService;
import com.fleetx.shared.dto.ApiResponse;
import com.fleetx.shared.dto.PageResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping({"/api/v1/fleet", "/api/fleet"})
public class FleetMaintenanceController {

    private final MaintenanceService maintenanceService;

    public FleetMaintenanceController(MaintenanceService maintenanceService) {
        this.maintenanceService = maintenanceService;
    }

    @GetMapping("/warnings")
    public ResponseEntity<ApiResponse<List<ServiceWarningResponseDto>>> getActiveWarnings() {
        List<ServiceWarningResponseDto> warnings = maintenanceService.getActiveWarnings();
        return ResponseEntity.ok(ApiResponse.ok("Active service warnings retrieved successfully.", warnings));
    }

    @GetMapping("/maintenance-records")
    public ResponseEntity<ApiResponse<PageResponse<MaintenanceRecordResponseDto>>> getAllMaintenanceRecords(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<MaintenanceRecordResponseDto> recordPage = maintenanceService.getAllRecords(pageable);

        PageResponse<MaintenanceRecordResponseDto> response = new PageResponse<>(
                recordPage.getContent(),
                recordPage.getNumber(),
                recordPage.getSize(),
                recordPage.getTotalElements(),
                recordPage.getTotalPages(),
                recordPage.isLast()
        );

        return ResponseEntity.ok(ApiResponse.ok("Fleet maintenance records retrieved successfully.", response));
    }
}
