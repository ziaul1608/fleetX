package com.fleetx.auth;

import com.fleetx.FleetxApplication;
import com.fleetx.auth.repository.UserRepository;
import com.fleetx.drivers.repository.DriverRepository;
import com.fleetx.drivers.repository.TripRepository;
import com.fleetx.fleet.repository.VehicleRepository;
import com.fleetx.shipments.repository.ShipmentRepository;
import com.fleetx.tracking.repository.GeofenceRepository;
import com.fleetx.tracking.repository.TrackingRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = FleetxApplication.class)
@ActiveProfiles("prod")
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:fleetx_prod_seeder_safety;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE;MODE=MySQL",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.flyway.enabled=false"
})
public class ProductionProfileSeederSafetyTest {

    @Value("${fleetx.demo.seed.enabled:false}")
    private boolean demoSeedEnabled;

    @Value("${fleetx.bootstrap.admin.enabled:false}")
    private boolean bootstrapAdminEnabled;

    @Value("${fleetx.bootstrap.admin.password-override:false}")
    private boolean passwordOverrideEnabled;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private VehicleRepository vehicleRepository;

    @Autowired
    private DriverRepository driverRepository;

    @Autowired
    private TripRepository tripRepository;

    @Autowired
    private ShipmentRepository shipmentRepository;

    @Autowired
    private GeofenceRepository geofenceRepository;

    @Autowired
    private TrackingRepository trackingRepository;

    @Test
    @DisplayName("Verify production configuration strictly disables demo seeders and bootstrap admin")
    void testProductionConfigurationDisablesSeedersAndBootstrap() {
        assertThat(demoSeedEnabled)
                .describedAs("fleetx.demo.seed.enabled MUST be false under prod profile")
                .isFalse();

        assertThat(bootstrapAdminEnabled)
                .describedAs("fleetx.bootstrap.admin.enabled MUST be false under prod profile")
                .isFalse();

        assertThat(passwordOverrideEnabled)
                .describedAs("fleetx.bootstrap.admin.password-override MUST be false under prod profile")
                .isFalse();
    }

    @Test
    @DisplayName("Verify production startup creates zero demo records across all business domains")
    void testProductionStartupCreatesZeroDemoRecords() {
        // 1. Identity domain: No demo staff or bootstrap admin
        assertThat(userRepository.findByEmail("admin@fleetx.local")).isEmpty();
        assertThat(userRepository.findByEmail("fleet.manager@fleetx.local")).isEmpty();
        assertThat(userRepository.findByEmail("dispatcher@fleetx.local")).isEmpty();
        assertThat(userRepository.findByEmail("driver@fleetx.local")).isEmpty();
        assertThat(userRepository.findByEmail("viewer@fleetx.local")).isEmpty();

        // 2. Fleet domain: No demo vehicles
        assertThat(vehicleRepository.existsByLicensePlate("MH-12-FX-1001")).isFalse();
        assertThat(vehicleRepository.existsByLicensePlate("MH-12-FX-1002")).isFalse();
        assertThat(vehicleRepository.count()).isZero();

        // 3. Driver & Trip domain: No demo drivers or trips
        assertThat(driverRepository.existsByLicenseNumber("DL-MH-2024-00101")).isFalse();
        assertThat(driverRepository.count()).isZero();
        assertThat(tripRepository.existsByTripCode("TRIP-2026-0001")).isFalse();
        assertThat(tripRepository.count()).isZero();

        // 4. Shipment domain: No demo shipments
        assertThat(shipmentRepository.existsByTrackingNumber("SHP-2026-0001")).isFalse();
        assertThat(shipmentRepository.count()).isZero();

        // 5. Tracking domain: No demo geofences or telemetry
        assertThat(geofenceRepository.count()).isZero();
        assertThat(trackingRepository.count()).isZero();
    }
}
