package com.fleetx.drivers.service;

import com.fleetx.drivers.dto.DriverPerformanceResponseDto;
import com.fleetx.drivers.entity.Driver;
import com.fleetx.drivers.entity.DriverPerformanceMetrics;
import com.fleetx.drivers.repository.DriverPerformanceMetricsRepository;
import com.fleetx.drivers.repository.DriverRepository;
import com.fleetx.shared.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;

@Service
public class DriverPerformanceService {

    private final DriverPerformanceMetricsRepository metricsRepository;
    private final DriverRepository driverRepository;

    public DriverPerformanceService(DriverPerformanceMetricsRepository metricsRepository,
                                    DriverRepository driverRepository) {
        this.metricsRepository = metricsRepository;
        this.driverRepository = driverRepository;
    }

    @Transactional(readOnly = true)
    public DriverPerformanceResponseDto getPerformanceByDriverId(Long driverId) {
        Driver driver = driverRepository.findById(driverId)
                .orElseThrow(() -> new ResourceNotFoundException("Driver", driverId));

        DriverPerformanceMetrics metrics = metricsRepository.findByDriverId(driverId)
                .orElseGet(() -> new DriverPerformanceMetrics(driverId));

        BigDecimal onTimeRate = calculateOnTimeRate(metrics.getTotalTripsCompleted(), metrics.getOnTimeDeliveries());
        BigDecimal ratingScore = calculateRatingScore(metrics.getTotalTripsCompleted(), metrics.getOnTimeDeliveries(), metrics.getSafetyViolationCount());

        return new DriverPerformanceResponseDto(
                driver.getId(),
                driver.getFullName(),
                metrics.getTotalTripsCompleted(),
                metrics.getOnTimeDeliveries(),
                onTimeRate,
                metrics.getSafetyViolationCount(),
                metrics.getTotalTripsCancelled(),
                metrics.getTotalDistanceKm(),
                ratingScore,
                metrics.getLastCalculatedAt()
        );
    }

    @Transactional
    public void recordTripCompleted(Long driverId, boolean onTime, BigDecimal distanceKm) {
        DriverPerformanceMetrics metrics = metricsRepository.findByDriverId(driverId)
                .orElseGet(() -> new DriverPerformanceMetrics(driverId));

        int completed = metrics.getTotalTripsCompleted() + 1;
        metrics.setTotalTripsCompleted(completed);

        if (onTime) {
            metrics.setOnTimeDeliveries(metrics.getOnTimeDeliveries() + 1);
        }

        if (distanceKm != null && distanceKm.compareTo(BigDecimal.ZERO) > 0) {
            metrics.setTotalDistanceKm(metrics.getTotalDistanceKm().add(distanceKm));
        }

        BigDecimal score = calculateRatingScore(completed, metrics.getOnTimeDeliveries(), metrics.getSafetyViolationCount());
        metrics.setRatingScore(score);
        metrics.setLastCalculatedAt(Instant.now());

        metricsRepository.save(metrics);
    }

    @Transactional
    public void recordTripCancelled(Long driverId) {
        DriverPerformanceMetrics metrics = metricsRepository.findByDriverId(driverId)
                .orElseGet(() -> new DriverPerformanceMetrics(driverId));

        metrics.setTotalTripsCancelled(metrics.getTotalTripsCancelled() + 1);
        metrics.setLastCalculatedAt(Instant.now());
        metricsRepository.save(metrics);
    }

    @Transactional
    public void recordSafetyViolation(Long driverId) {
        DriverPerformanceMetrics metrics = metricsRepository.findByDriverId(driverId)
                .orElseGet(() -> new DriverPerformanceMetrics(driverId));

        int violations = metrics.getSafetyViolationCount() + 1;
        metrics.setSafetyViolationCount(violations);

        BigDecimal score = calculateRatingScore(metrics.getTotalTripsCompleted(), metrics.getOnTimeDeliveries(), violations);
        metrics.setRatingScore(score);
        metrics.setLastCalculatedAt(Instant.now());
        metricsRepository.save(metrics);
    }

    /**
     * Calculates the on-time delivery rate percentage.
     * Safely returns 100.00% if zero completed trips to prevent division-by-zero.
     */
    public BigDecimal calculateOnTimeRate(int totalTrips, int onTimeTrips) {
        if (totalTrips <= 0) {
            return new BigDecimal("100.00");
        }
        double rate = ((double) onTimeTrips / totalTrips) * 100.0;
        return BigDecimal.valueOf(Math.min(100.0, Math.max(0.0, rate))).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Calculates the composite rating score (0.00 to 100.00).
     * Formula:
     * - Default base score: 100.00
     * - If totalTrips == 0: returns 100.00 (safe against zero division).
     * - Late penalty: (1.0 - (onTimeTrips / totalTrips)) * 40.0
     * - Safety violation penalty: min(40.0, violations * 10.0)
     * - Composite score = max(0.0, min(100.0, 100.0 - latePenalty - safetyPenalty))
     */
    public BigDecimal calculateRatingScore(int totalTrips, int onTimeTrips, int safetyViolations) {
        if (totalTrips <= 0) {
            double safetyPenalty = Math.min(40.0, safetyViolations * 10.0);
            double score = Math.max(0.0, 100.0 - safetyPenalty);
            return BigDecimal.valueOf(score).setScale(2, RoundingMode.HALF_UP);
        }

        double onTimeFraction = Math.min(1.0, Math.max(0.0, (double) onTimeTrips / totalTrips));
        double latePenalty = (1.0 - onTimeFraction) * 40.0;
        double safetyPenalty = Math.min(40.0, safetyViolations * 10.0);

        double composite = Math.max(0.0, Math.min(100.0, 100.0 - latePenalty - safetyPenalty));
        return BigDecimal.valueOf(composite).setScale(2, RoundingMode.HALF_UP);
    }
}
