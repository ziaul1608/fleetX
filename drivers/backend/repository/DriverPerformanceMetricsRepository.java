package com.fleetx.drivers.repository;

import com.fleetx.drivers.entity.DriverPerformanceMetrics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DriverPerformanceMetricsRepository extends JpaRepository<DriverPerformanceMetrics, Long> {
    Optional<DriverPerformanceMetrics> findByDriverId(Long driverId);
}
