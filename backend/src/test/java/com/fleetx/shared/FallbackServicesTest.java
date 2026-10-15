package com.fleetx.shared;

import com.fleetx.FleetxApplication;
import com.fleetx.shared.contract.TripLookupService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = FleetxApplication.class)
@ActiveProfiles("test")
public class FallbackServicesTest {

    @Autowired
    private TripLookupService tripLookupService;

    @Test
    @DisplayName("TripLookupService fallback bean should return safe empty values")
    void fallbackBeanShouldReturnSafeEmptyValues() {
        assertThat(tripLookupService).isNotNull();
        assertThat(tripLookupService.findTripSummaryById(999L)).isEmpty();
        assertThat(tripLookupService.isTripActive(999L)).isFalse();
    }
}
