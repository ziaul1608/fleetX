package com.fleetx.drivers;

import com.fleetx.FleetxApplication;
import com.fleetx.drivers.dto.DriverPerformanceResponseDto;
import com.fleetx.drivers.entity.Driver;
import com.fleetx.drivers.entity.DriverPerformanceMetrics;
import com.fleetx.drivers.repository.DriverPerformanceMetricsRepository;
import com.fleetx.drivers.repository.DriverRepository;
import com.fleetx.drivers.service.DriverPerformanceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = FleetxApplication.class)
@ActiveProfiles("test")
public class DriverPerformanceTest {

    @Autowired
    private DriverRepository driverRepository;

    @Autowired
    private DriverPerformanceMetricsRepository metricsRepository;

    @Autowired
    private DriverPerformanceService performanceService;

    private Long testDriverId;

    @BeforeEach
    void setup() {
        metricsRepository.deleteAll();
        driverRepository.deleteAll();

        Driver driver = new Driver();
        driver.setFullName("Kavita Rao");
        driver.setPhoneNumber("+91 98765 33445");
        driver.setLicenseNumber("MH-12-KAVITA-01");
        driver.setLicenseCategory("HEAVY");
        driver.setLicenseExpiryDate(LocalDate.now().plusYears(4));
        driver.setAvailabilityStatus("AVAILABLE");
        Driver saved = driverRepository.save(driver);
        testDriverId = saved.getId();
    }

    @Test
    @DisplayName("Zero completed trips calculates 100.00 score and 100% on-time rate without division-by-zero")
    void performance_zeroCompletedTrips_safeCalculation() {
        DriverPerformanceResponseDto perf = performanceService.getPerformanceByDriverId(testDriverId);

        assertThat(perf.totalTripsCompleted()).isEqualTo(0);
        assertThat(perf.onTimeDeliveries()).isEqualTo(0);
        assertThat(perf.onTimeDeliveryRate()).isEqualByComparingTo("100.00");
        assertThat(perf.ratingScore()).isEqualByComparingTo("100.00");
    }

    @Test
    @DisplayName("Calculates performance score accurately from persisted trips and on-time deliveries")
    void performance_withCompletedTrips_accurateFormula() {
        // Record 10 trips: 8 on-time, 2 late
        for (int i = 0; i < 8; i++) {
            performanceService.recordTripCompleted(testDriverId, true, new BigDecimal("120.00"));
        }
        for (int i = 0; i < 2; i++) {
            performanceService.recordTripCompleted(testDriverId, false, new BigDecimal("80.00"));
        }

        DriverPerformanceResponseDto perf = performanceService.getPerformanceByDriverId(testDriverId);

        assertThat(perf.totalTripsCompleted()).isEqualTo(10);
        assertThat(perf.onTimeDeliveries()).isEqualTo(8);
        assertThat(perf.onTimeDeliveryRate()).isEqualByComparingTo("80.00");
        // On-time = 8/10 = 80%. Late penalty = (1 - 0.8) * 40 = 8.0. Score = 100 - 8 = 92.00
        assertThat(perf.ratingScore()).isEqualByComparingTo("92.00");
        assertThat(perf.totalDistanceKm()).isEqualByComparingTo("1120.00");
    }

    @Test
    @DisplayName("Applies safety violation deductions to composite score")
    void performance_withSafetyViolations_deductsScore() {
        // 5 trips all on time
        for (int i = 0; i < 5; i++) {
            performanceService.recordTripCompleted(testDriverId, true, new BigDecimal("100.00"));
        }
        // 2 safety violations
        performanceService.recordSafetyViolation(testDriverId);
        performanceService.recordSafetyViolation(testDriverId);

        DriverPerformanceResponseDto perf = performanceService.getPerformanceByDriverId(testDriverId);

        assertThat(perf.totalTripsCompleted()).isEqualTo(5);
        assertThat(perf.safetyViolationCount()).isEqualTo(2);
        // Late penalty = 0. Safety penalty = 2 * 10 = 20.0. Score = 100 - 20 = 80.00
        assertThat(perf.ratingScore()).isEqualByComparingTo("80.00");
    }
}
