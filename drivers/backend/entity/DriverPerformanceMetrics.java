package com.fleetx.drivers.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "driver_performance_metrics")
public class DriverPerformanceMetrics {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "driver_id", nullable = false, unique = true)
    private Long driverId;

    @Column(name = "total_trips_completed", nullable = false)
    private Integer totalTripsCompleted = 0;

    @Column(name = "on_time_deliveries", nullable = false)
    private Integer onTimeDeliveries = 0;

    @Column(name = "safety_violation_count", nullable = false)
    private Integer safetyViolationCount = 0;

    @Column(name = "total_trips_cancelled", nullable = false)
    private Integer totalTripsCancelled = 0;

    @Column(name = "total_distance_km", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalDistanceKm = BigDecimal.ZERO;

    @Column(name = "rating_score", nullable = false, precision = 5, scale = 2)
    private BigDecimal ratingScore = new BigDecimal("100.00");

    @Column(name = "last_calculated_at")
    private Instant lastCalculatedAt;

    public DriverPerformanceMetrics() {}

    public DriverPerformanceMetrics(Long driverId) {
        this.driverId = driverId;
        this.totalTripsCompleted = 0;
        this.onTimeDeliveries = 0;
        this.safetyViolationCount = 0;
        this.totalTripsCancelled = 0;
        this.totalDistanceKm = BigDecimal.ZERO;
        this.ratingScore = new BigDecimal("100.00");
        this.lastCalculatedAt = Instant.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getDriverId() { return driverId; }
    public void setDriverId(Long driverId) { this.driverId = driverId; }

    public Integer getTotalTripsCompleted() { return totalTripsCompleted; }
    public void setTotalTripsCompleted(Integer totalTripsCompleted) { this.totalTripsCompleted = totalTripsCompleted; }

    public Integer getOnTimeDeliveries() { return onTimeDeliveries; }
    public void setOnTimeDeliveries(Integer onTimeDeliveries) { this.onTimeDeliveries = onTimeDeliveries; }

    public Integer getSafetyViolationCount() { return safetyViolationCount; }
    public void setSafetyViolationCount(Integer safetyViolationCount) { this.safetyViolationCount = safetyViolationCount; }

    public Integer getTotalTripsCancelled() { return totalTripsCancelled; }
    public void setTotalTripsCancelled(Integer totalTripsCancelled) { this.totalTripsCancelled = totalTripsCancelled; }

    public BigDecimal getTotalDistanceKm() { return totalDistanceKm; }
    public void setTotalDistanceKm(BigDecimal totalDistanceKm) { this.totalDistanceKm = totalDistanceKm; }

    public BigDecimal getRatingScore() { return ratingScore; }
    public void setRatingScore(BigDecimal ratingScore) { this.ratingScore = ratingScore; }

    public Instant getLastCalculatedAt() { return lastCalculatedAt; }
    public void setLastCalculatedAt(Instant lastCalculatedAt) { this.lastCalculatedAt = lastCalculatedAt; }
}
