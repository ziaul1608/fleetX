package com.fleetx.fleet.service;

import com.fleetx.fleet.dto.*;
import com.fleetx.fleet.entity.FuelType;
import com.fleetx.fleet.entity.Vehicle;
import com.fleetx.fleet.entity.VehicleStatus;
import com.fleetx.fleet.entity.VehicleType;
import com.fleetx.fleet.repository.ServiceWarningRepository;
import com.fleetx.fleet.repository.VehicleRepository;
import com.fleetx.shared.contract.TripLookupService;
import com.fleetx.shared.contract.VehicleLookupService;
import com.fleetx.shared.dto.VehicleSummaryDto;
import com.fleetx.shared.exception.DomainException;
import com.fleetx.shared.exception.ResourceConflictException;
import com.fleetx.shared.exception.ResourceNotFoundException;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class VehicleService implements VehicleLookupService {

    private final VehicleRepository vehicleRepository;
    private final ServiceWarningRepository warningRepository;
    private final FuelService fuelService;
    private final MaintenanceService maintenanceService;
    private final TripLookupService tripLookupService;

    public VehicleService(VehicleRepository vehicleRepository,
                          ServiceWarningRepository warningRepository,
                          @Lazy FuelService fuelService,
                          @Lazy MaintenanceService maintenanceService,
                          @Lazy TripLookupService tripLookupService) {
        this.vehicleRepository = vehicleRepository;
        this.warningRepository = warningRepository;
        this.fuelService = fuelService;
        this.maintenanceService = maintenanceService;
        this.tripLookupService = tripLookupService;
    }

    @Transactional
    public VehicleResponseDto createVehicle(VehicleCreateRequest request) {
        String normalizedVin = request.getVin().trim().toUpperCase();
        String normalizedPlate = request.getLicensePlate().trim().toUpperCase();

        if (vehicleRepository.existsByVin(normalizedVin)) {
            throw new ResourceConflictException("A vehicle with VIN '" + normalizedVin + "' already exists.");
        }
        if (vehicleRepository.existsByLicensePlate(normalizedPlate)) {
            throw new ResourceConflictException("A vehicle with registration plate '" + normalizedPlate + "' already exists.");
        }

        // Validate enums
        VehicleType type = VehicleType.fromString(request.getVehicleType());
        FuelType fuelType = FuelType.fromString(request.getFuelType());
        VehicleStatus status = request.getStatus() != null && !request.getStatus().isBlank()
                ? VehicleStatus.fromString(request.getStatus())
                : VehicleStatus.AVAILABLE;

        Vehicle vehicle = new Vehicle(
                normalizedVin,
                normalizedPlate,
                request.getMake(),
                request.getModel(),
                request.getModelYear(),
                type.name(),
                fuelType.name(),
                request.getCapacityKg(),
                request.getCapacityVolumeM3(),
                request.getOdometerKm(),
                status.name()
        );

        Vehicle saved = vehicleRepository.save(vehicle);
        return mapToDto(saved);
    }

    @Transactional(readOnly = true)
    public Page<VehicleResponseDto> findVehicles(String status, String type, Boolean active, String search, Pageable pageable) {
        String statusFilter = (status != null && !status.isBlank()) ? status.trim() : null;
        String typeFilter = (type != null && !type.isBlank()) ? type.trim() : null;
        String searchFilter = (search != null && !search.isBlank()) ? search.trim() : null;

        return vehicleRepository.findWithFilters(statusFilter, typeFilter, active, searchFilter, pageable)
                .map(this::mapToDto);
    }

    @Transactional(readOnly = true)
    public List<VehicleResponseDto> findAllVehiclesList() {
        return vehicleRepository.findAll().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public VehicleResponseDto getVehicleById(Long id) {
        Vehicle vehicle = vehicleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle", id));
        return mapToDto(vehicle);
    }

    @Transactional
    public VehicleResponseDto updateVehicle(Long id, VehicleUpdateRequest request) {
        Vehicle vehicle = vehicleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle", id));

        // Validate enums
        VehicleType type = VehicleType.fromString(request.getVehicleType());
        FuelType fuelType = FuelType.fromString(request.getFuelType());
        VehicleStatus targetStatus = VehicleStatus.fromString(request.getStatus());

        if (targetStatus == VehicleStatus.MAINTENANCE || targetStatus == VehicleStatus.RETIRED) {
            if (tripLookupService.hasActiveTripForVehicle(id)) {
                throw new DomainException(
                        "Cannot transition vehicle #" + id + " to " + targetStatus + " because it is currently assigned to an active trip.",
                        HttpStatus.CONFLICT,
                        "VEHICLE_HAS_ACTIVE_TRIP"
                );
            }
        }

        vehicle.setMake(request.getMake());
        vehicle.setModel(request.getModel());
        vehicle.setModelYear(request.getModelYear());
        vehicle.setVehicleType(type.name());
        vehicle.setFuelType(fuelType.name());
        vehicle.setCapacityKg(request.getCapacityKg());
        vehicle.setCapacityVolumeM3(request.getCapacityVolumeM3());
        vehicle.setStatus(targetStatus.name());
        vehicle.setActive(request.getActive());

        Vehicle updated = vehicleRepository.save(vehicle);
        return mapToDto(updated);
    }

    @Transactional
    public VehicleResponseDto updateVehicleStatus(Long id, VehicleStatusUpdateRequest request) {
        Vehicle vehicle = vehicleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle", id));

        VehicleStatus targetStatus = VehicleStatus.fromString(request.getStatus());

        if (targetStatus == VehicleStatus.MAINTENANCE || targetStatus == VehicleStatus.RETIRED) {
            if (tripLookupService.hasActiveTripForVehicle(id)) {
                throw new DomainException(
                        "Cannot transition vehicle #" + id + " to " + targetStatus + " because it is currently assigned to an active trip.",
                        HttpStatus.CONFLICT,
                        "VEHICLE_HAS_ACTIVE_TRIP"
                );
            }
        }

        vehicle.setStatus(targetStatus.name());

        Vehicle updated = vehicleRepository.save(vehicle);
        return mapToDto(updated);
    }

    @Transactional
    public VehicleResponseDto updateOdometer(Long id, Double newOdometerKm) {
        Vehicle vehicle = vehicleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle", id));

        if (newOdometerKm < vehicle.getOdometerKm()) {
            throw new DomainException(
                    "New odometer reading (" + newOdometerKm + " km) cannot be less than current reading (" +
                    vehicle.getOdometerKm() + " km).",
                    org.springframework.http.HttpStatus.BAD_REQUEST, "INVALID_ODOMETER_VALUE");
        }

        vehicle.setOdometerKm(newOdometerKm);
        Vehicle saved = vehicleRepository.save(vehicle);

        // Check for upcoming or overdue maintenance
        maintenanceService.checkAndPublishWarnings(saved);

        return mapToDto(saved);
    }

    // --- Contract Implementation: VehicleLookupService ---

    @Override
    @Transactional(readOnly = true)
    public Optional<VehicleSummaryDto> findVehicleSummaryById(Long vehicleId) {
        return vehicleRepository.findById(vehicleId)
                .map(v -> new VehicleSummaryDto(
                        v.getId(),
                        v.getVin(),
                        v.getLicensePlate(),
                        v.getMake(),
                        v.getModel(),
                        v.getStatus(),
                        v.getOdometerKm()
                ));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isVehicleAvailableForTrip(Long vehicleId) {
        Optional<Vehicle> vehicleOpt = vehicleRepository.findById(vehicleId);
        if (vehicleOpt.isEmpty()) {
            return false;
        }

        Vehicle vehicle = vehicleOpt.get();
        if (!Boolean.TRUE.equals(vehicle.getActive())) {
            return false;
        }

        String status = vehicle.getStatus().toUpperCase();
        boolean isStatusEligible = "AVAILABLE".equals(status) || "ACTIVE".equals(status) || "IDLE".equals(status);
        if (!isStatusEligible) {
            return false;
        }

        // Vehicle must have NO unresolved CRITICAL service warnings
        boolean hasCriticalWarning = warningRepository.existsByVehicleIdAndSeverityAndResolvedFalse(
                vehicleId, "CRITICAL");

        return !hasCriticalWarning;
    }

    @Override
    @Transactional
    public void updateVehicleOperationalStatus(Long vehicleId, String status) {
        VehicleStatus validated = VehicleStatus.fromString(status);
        vehicleRepository.findById(vehicleId).ifPresent(v -> {
            v.setStatus(validated.name());
            vehicleRepository.save(v);
        });
    }

    private VehicleResponseDto mapToDto(Vehicle v) {
        Double avgEfficiency = fuelService.calculateAverageEfficiency(v.getId());
        boolean serviceDue = maintenanceService.hasServiceDue(v.getId());

        return new VehicleResponseDto(
                v.getId(),
                v.getVin(),
                v.getLicensePlate(),
                v.getMake(),
                v.getModel(),
                v.getModelYear(),
                v.getVehicleType(),
                v.getFuelType(),
                v.getCapacityKg(),
                v.getCapacityVolumeM3(),
                v.getOdometerKm(),
                v.getStatus(),
                v.getActive(),
                avgEfficiency,
                serviceDue,
                v.getCreatedAt()
        );
    }
}
