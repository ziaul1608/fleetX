package com.fleetx.fleet.service;

import com.fleetx.fleet.dto.FuelRecordCreateRequest;
import com.fleetx.fleet.dto.FuelRecordResponseDto;
import com.fleetx.fleet.entity.FuelRecord;
import com.fleetx.fleet.entity.Vehicle;
import com.fleetx.fleet.repository.FuelRecordRepository;
import com.fleetx.fleet.repository.VehicleRepository;
import com.fleetx.shared.context.TenantSecurityContext;
import com.fleetx.shared.exception.DomainException;
import com.fleetx.shared.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
public class FuelService {

    private static final Logger log = LoggerFactory.getLogger(FuelService.class);

    private final FuelRecordRepository fuelRepository;
    private final VehicleRepository vehicleRepository;
    private final MaintenanceService maintenanceService;

    public FuelService(FuelRecordRepository fuelRepository,
                       VehicleRepository vehicleRepository,
                       MaintenanceService maintenanceService) {
        this.fuelRepository = fuelRepository;
        this.vehicleRepository = vehicleRepository;
        this.maintenanceService = maintenanceService;
    }

    /**
     * Records a new fuel transaction, calculates total cost, and computes fuel efficiency.
     *
     * Fuel Efficiency Formula:
     * Efficiency (km/L) = Delta Distance (km) / Fuel Volume (L)
     * Where:
     *   Delta Distance = Odometer_current - Odometer_previous
     *
     * Edge case handling:
     * 1. If Delta Distance <= 0 (e.g. duplicate odometer, top-up without driving, or odometer reset),
     *    efficiency is set to NULL to prevent mathematical divide-by-zero or negative distortion.
     * 2. If liters <= 0, rejection is enforced via validation.
     * 3. First fueling evaluates distance against vehicle's initial baseline odometer.
     */
    @Transactional
    public FuelRecordResponseDto recordFueling(Long vehicleId, FuelRecordCreateRequest request) {
        Long tenantId = TenantSecurityContext.requireTenantId();
        Vehicle vehicle = vehicleRepository.findByIdAndTenantId(vehicleId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle", vehicleId));

        if (!Boolean.TRUE.equals(vehicle.getActive())) {
            throw new DomainException("Cannot log fuel records for a deactivated vehicle.",
                    org.springframework.http.HttpStatus.BAD_REQUEST, "VEHICLE_INACTIVE");
        }

        double liters = request.getLiters();
        if (liters <= 0) {
            throw new DomainException("Fuel liters must be greater than zero.",
                    org.springframework.http.HttpStatus.BAD_REQUEST, "INVALID_FUEL_QUANTITY");
        }

        BigDecimal pricePerLiter = request.getPricePerLiter();
        BigDecimal totalCost = request.getTotalCost();

        // Calculate total cost or price per liter in Java using validated inputs
        if (pricePerLiter != null && totalCost == null) {
            totalCost = pricePerLiter.multiply(BigDecimal.valueOf(liters)).setScale(2, RoundingMode.HALF_UP);
        } else if (totalCost != null && pricePerLiter == null) {
            pricePerLiter = totalCost.divide(BigDecimal.valueOf(liters), 2, RoundingMode.HALF_UP);
        } else if (pricePerLiter != null && totalCost != null) {
            // Verify approximate consistency within rounding error
            BigDecimal calculated = pricePerLiter.multiply(BigDecimal.valueOf(liters)).setScale(2, RoundingMode.HALF_UP);
            if (calculated.subtract(totalCost).abs().compareTo(BigDecimal.valueOf(0.50)) > 0) {
                totalCost = calculated;
            }
        } else {
            throw new DomainException("Either pricePerLiter or totalCost must be provided.",
                    org.springframework.http.HttpStatus.BAD_REQUEST, "MISSING_COST_PARAMETERS");
        }

        double currentOdometer = request.getOdometerAtFueling();

        // Find preceding fueling record to calculate distance delta
        Optional<FuelRecord> latestRecord =
                fuelRepository.findTopByVehicleIdOrderByOdometerAtFuelingDescFuelDateDesc(vehicleId);

        Double previousOdometer;
        if (latestRecord.isPresent()) {
            previousOdometer = latestRecord.get().getOdometerAtFueling();
        } else {
            // Fall back to vehicle initial odometer
            previousOdometer = vehicle.getOdometerKm();
        }

        Double efficiency = null;
        if (previousOdometer != null) {
            double deltaDistance = currentOdometer - previousOdometer;
            if (deltaDistance > 0.0) {
                efficiency = BigDecimal.valueOf(deltaDistance / liters)
                        .setScale(2, RoundingMode.HALF_UP)
                        .doubleValue();
            } else {
                log.info("Zero or non-positive distance delta ({} km) detected for vehicle #{}; efficiency recorded as null.",
                        deltaDistance, vehicleId);
            }
        }

        Instant fuelDate = request.getFuelDate() != null ? request.getFuelDate() : Instant.now();

        FuelRecord record = new FuelRecord(
                vehicle,
                liters,
                pricePerLiter,
                totalCost,
                currentOdometer,
                request.getFuelStation(),
                fuelDate
        );
        record.setEfficiencyKmPerLiter(efficiency);
        record.setTenantId(tenantId);

        FuelRecord saved = fuelRepository.save(record);

        // Update vehicle odometer if new reading is higher
        if (currentOdometer > vehicle.getOdometerKm()) {
            vehicle.setOdometerKm(currentOdometer);
            vehicleRepository.save(vehicle);

            // Check if updated odometer triggers upcoming or overdue service warnings
            maintenanceService.checkAndPublishWarnings(vehicle);
        }

        return mapToDto(saved);
    }

    @Transactional(readOnly = true)
    public Page<FuelRecordResponseDto> getRecordsByVehicle(Long vehicleId, Pageable pageable) {
        Long tenantId = TenantSecurityContext.requireTenantId();
        Vehicle vehicle = vehicleRepository.findByIdAndTenantId(vehicleId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle", vehicleId));
        return fuelRepository.findByVehicleIdOrderByFuelDateDesc(vehicle.getId(), pageable)
                .map(this::mapToDto);
    }

    /**
     * Calculates the cumulative lifetime average fuel efficiency for a vehicle.
     * Sum of all valid distance deltas / Sum of corresponding liters.
     */
    @Transactional(readOnly = true)
    public Double calculateAverageEfficiency(Long vehicleId) {
        List<FuelRecord> records = fuelRepository.findByVehicleIdOrderByOdometerAtFuelingAsc(vehicleId);
        if (records.size() < 2) {
            // Single fueling cannot establish distance interval without prior baseline
            return records.stream()
                    .filter(r -> r.getEfficiencyKmPerLiter() != null)
                    .map(FuelRecord::getEfficiencyKmPerLiter)
                    .findFirst()
                    .orElse(null);
        }

        double totalDeltaDistance = 0.0;
        double totalLiters = 0.0;

        for (int i = 1; i < records.size(); i++) {
            double prevOdo = records.get(i - 1).getOdometerAtFueling();
            double currOdo = records.get(i).getOdometerAtFueling();
            double delta = currOdo - prevOdo;
            double liters = records.get(i).getLiters();

            if (delta > 0.0 && liters > 0.0) {
                totalDeltaDistance += delta;
                totalLiters += liters;
            }
        }

        if (totalLiters > 0.0 && totalDeltaDistance > 0.0) {
            return BigDecimal.valueOf(totalDeltaDistance / totalLiters)
                    .setScale(2, RoundingMode.HALF_UP)
                    .doubleValue();
        }

        return null;
    }

    private FuelRecordResponseDto mapToDto(FuelRecord r) {
        return new FuelRecordResponseDto(
                r.getId(),
                r.getVehicle().getId(),
                r.getVehicle().getLicensePlate(),
                r.getLiters(),
                r.getPricePerLiter(),
                r.getTotalCost(),
                r.getOdometerAtFueling(),
                r.getFuelStation(),
                r.getFuelDate(),
                r.getEfficiencyKmPerLiter()
        );
    }
}
