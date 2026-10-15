package com.fleetx.tracking.controller;

import com.fleetx.shared.dto.ApiResponse;
import com.fleetx.shared.dto.PageResponse;
import com.fleetx.tracking.dto.*;
import com.fleetx.tracking.service.TrackingService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@RestController
@RequestMapping({"/api/v1/tracking", "/api/tracking"})
public class TrackingController {

    private final TrackingService trackingService;

    public TrackingController(TrackingService trackingService) {
        this.trackingService = trackingService;
    }

    // --- Control Tower Overview ---

    @GetMapping("/control-tower/summary")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getControlTowerSummary() {
        Map<String, Object> summary = trackingService.getControlTowerSummary();
        return ResponseEntity.ok(ApiResponse.ok("Control tower summary retrieved.", summary));
    }

    // --- Simulated GPS Tracking Engine ---

    @GetMapping({"/simulator/sessions", "/simulator/status"})
    public ResponseEntity<ApiResponse<List<SimulatorSessionDto>>> getSimulatorSessions() {
        List<SimulatorSessionDto> sessions = trackingService.getSimulatorSessions();
        return ResponseEntity.ok(ApiResponse.ok("Simulated GPS sessions retrieved.", sessions));
    }

    @PostMapping("/simulator/start")
    @PreAuthorize("hasAnyRole('ADMIN', 'FLEET_MANAGER', 'DISPATCHER')")
    public ResponseEntity<ApiResponse<SimulatorSessionDto>> startSimulation(
            @Valid @RequestBody SimulatorStartRequest request
    ) {
        SimulatorSessionDto session = trackingService.startSimulation(request);
        return ResponseEntity.ok(ApiResponse.ok("Simulated GPS worker initiated.", session));
    }

    @PostMapping("/simulator/{vehicleId}/pause")
    @PreAuthorize("hasAnyRole('ADMIN', 'FLEET_MANAGER', 'DISPATCHER')")
    public ResponseEntity<ApiResponse<SimulatorSessionDto>> pauseSimulation(@PathVariable Long vehicleId) {
        SimulatorSessionDto session = trackingService.pauseSimulation(vehicleId);
        return ResponseEntity.ok(ApiResponse.ok("Simulated GPS worker paused.", session));
    }

    @PostMapping("/simulator/{vehicleId}/resume")
    @PreAuthorize("hasAnyRole('ADMIN', 'FLEET_MANAGER', 'DISPATCHER')")
    public ResponseEntity<ApiResponse<SimulatorSessionDto>> resumeSimulation(@PathVariable Long vehicleId) {
        SimulatorSessionDto session = trackingService.resumeSimulation(vehicleId);
        return ResponseEntity.ok(ApiResponse.ok("Simulated GPS worker resumed.", session));
    }

    @PostMapping("/simulator/{vehicleId}/stop")
    @PreAuthorize("hasAnyRole('ADMIN', 'FLEET_MANAGER', 'DISPATCHER')")
    public ResponseEntity<ApiResponse<SimulatorSessionDto>> stopSimulation(@PathVariable Long vehicleId) {
        SimulatorSessionDto session = trackingService.stopSimulation(vehicleId);
        return ResponseEntity.ok(ApiResponse.ok("Simulated GPS worker stopped.", session));
    }

    @PostMapping("/simulator/{vehicleId}/step")
    @PreAuthorize("hasAnyRole('ADMIN', 'FLEET_MANAGER', 'DISPATCHER')")
    public ResponseEntity<ApiResponse<SimulatorSessionDto>> stepSimulation(@PathVariable Long vehicleId) {
        SimulatorSessionDto session = trackingService.stepSimulation(vehicleId);
        return ResponseEntity.ok(ApiResponse.ok("Simulated GPS worker stepped forward by one tick.", session));
    }

    @PostMapping("/telemetry")
    @PreAuthorize("hasAnyRole('ADMIN', 'FLEET_MANAGER', 'DISPATCHER', 'DRIVER')")
    public ResponseEntity<ApiResponse<TelemetryPingDto>> ingestSimulatedTelemetry(
            @RequestBody TelemetryPingDto ping
    ) {
        TelemetryPingDto saved = trackingService.ingestSimulatedTelemetry(ping);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Simulated telemetry ping recorded and broadcast.", saved));
    }

    @GetMapping("/vehicles/{vehicleId}/history")
    public ResponseEntity<ApiResponse<List<TelemetryPingDto>>> getVehicleHistory(
            @PathVariable Long vehicleId,
            @RequestParam(defaultValue = "50") int limit
    ) {
        List<TelemetryPingDto> history = trackingService.getVehicleTrackingHistory(vehicleId, limit);
        return ResponseEntity.ok(ApiResponse.ok("Vehicle simulated telemetry history retrieved.", history));
    }

    // --- Geofence Management ---

    @PostMapping("/geofences")
    @PreAuthorize("hasAnyRole('ADMIN', 'FLEET_MANAGER', 'DISPATCHER')")
    public ResponseEntity<ApiResponse<GeofenceResponseDto>> createGeofence(
            @Valid @RequestBody GeofenceCreateRequest request
    ) {
        GeofenceResponseDto created = trackingService.createGeofence(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Geofence boundary created.", created));
    }

    @GetMapping("/geofences")
    public ResponseEntity<ApiResponse<List<GeofenceResponseDto>>> listGeofences(
            @RequestParam(required = false) Boolean activeOnly
    ) {
        List<GeofenceResponseDto> list = trackingService.listGeofences(activeOnly);
        return ResponseEntity.ok(ApiResponse.ok("Geofence boundaries retrieved.", list));
    }

    @PatchMapping("/geofences/{id}/toggle")
    @PreAuthorize("hasAnyRole('ADMIN', 'FLEET_MANAGER', 'DISPATCHER')")
    public ResponseEntity<ApiResponse<GeofenceResponseDto>> toggleGeofence(
            @PathVariable Long id,
            @RequestParam boolean active
    ) {
        GeofenceResponseDto updated = trackingService.toggleGeofence(id, active);
        return ResponseEntity.ok(ApiResponse.ok("Geofence status updated.", updated));
    }

    // --- Alerts Management ---

    @GetMapping("/alerts")
    public ResponseEntity<ApiResponse<PageResponse<AlertResponseDto>>> listAlerts(
            @RequestParam(required = false) String alertType,
            @RequestParam(required = false) String severity,
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) Boolean acknowledged,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        PageResponse<AlertResponseDto> alerts = trackingService.listAlerts(
                alertType, severity, entityType, acknowledged, page, size
        );
        return ResponseEntity.ok(ApiResponse.ok("Operational alerts retrieved.", alerts));
    }

    @RequestMapping(value = "/alerts/{id}/acknowledge", method = {RequestMethod.POST, RequestMethod.PATCH})
    @PreAuthorize("hasAnyRole('ADMIN', 'FLEET_MANAGER', 'DISPATCHER')")
    public ResponseEntity<ApiResponse<AlertResponseDto>> acknowledgeAlert(
            @PathVariable Long id,
            Authentication authentication
    ) {
        String actor = (authentication != null && authentication.getName() != null)
                ? authentication.getName()
                : "operator";
        AlertResponseDto updated = trackingService.acknowledgeAlert(id, actor);
        return ResponseEntity.ok(ApiResponse.ok("Operational alert acknowledged.", updated));
    }

    // --- Control Tower Reporting & Safe CSV Export ---

    @GetMapping("/reports/summary")
    @PreAuthorize("hasAnyRole('ADMIN', 'FLEET_MANAGER', 'DISPATCHER', 'VIEWER')")
    public ResponseEntity<ApiResponse<ReportSummaryDto>> getReportSummary(
            @RequestParam(defaultValue = "TRIPS") String reportType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String status
    ) {
        ReportSummaryDto report = trackingService.getReportSummary(reportType, startDate, endDate, status);
        return ResponseEntity.ok(ApiResponse.ok("Report summary generated.", report));
    }

    @GetMapping("/reports/export-csv")
    @PreAuthorize("hasAnyRole('ADMIN', 'FLEET_MANAGER', 'DISPATCHER', 'VIEWER')")
    public ResponseEntity<byte[]> exportReportCsv(
            @RequestParam(defaultValue = "TRIPS") String reportType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String status
    ) {
        String csvContent = trackingService.exportReportCsv(reportType, startDate, endDate, status);
        String safeFileType = reportType.trim().toLowerCase(Locale.US).replaceAll("[^a-z0-9_]", "");
        String filename = "fleetx_" + (safeFileType.isEmpty() ? "trips" : safeFileType) + "_report.csv";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(csvContent.getBytes(StandardCharsets.UTF_8));
    }
}
