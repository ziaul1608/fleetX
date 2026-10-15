package com.fleetx.integration;

import com.fleetx.drivers.service.DriverService;
import com.fleetx.drivers.service.TripService;
import com.fleetx.fleet.service.VehicleService;
import com.fleetx.shared.contract.*;
import com.fleetx.shipments.service.ShipmentService;
import com.fleetx.tracking.service.TrackingService;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.output.MigrateResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:fleetx_flyway_schema_test;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE;MODE=MySQL;DATABASE_TO_LOWER=TRUE",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.flyway.enabled=true",
        "spring.flyway.locations=classpath:db/migration",
        "fleetx.demo.seed.enabled=false"
})
@ActiveProfiles("test")
class FlywayMigrationAndArchitectureTest {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private Flyway flyway;

    @Autowired
    private VehicleLookupService vehicleLookupService;

    @Autowired
    private DriverLookupService driverLookupService;

    @Autowired
    private TripLookupService tripLookupService;

    @Autowired
    private ShipmentLookupService shipmentLookupService;

    @Autowired
    private AlertPublisher alertPublisher;

    @Test
    @DisplayName("All 10 Flyway migrations (V100..V501) execute in order, pass Hibernate ddl-auto=validate, and restart with zero schema drift")
    void flywayMigrationsAndRestartValidation() {
        MigrationInfo[] applied = flyway.info().applied();
        assertThat(applied).hasSize(10);

        List<String> versions = Arrays.stream(applied)
                .map(info -> info.getVersion().getVersion())
                .toList();
        assertThat(versions).containsExactly(
                "100", "101",
                "200", "201",
                "300", "301",
                "400", "401",
                "500", "501"
        );

        // Simulate application restart: re-running migrate and validate on existing schema
        Flyway restartFlyway = Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .validateOnMigrate(true)
                .load();

        MigrateResult secondRun = restartFlyway.migrate();
        assertThat(secondRun.migrationsExecuted).isZero();
        restartFlyway.validate();
    }

    @Test
    @DisplayName("All 5 shared contract interfaces resolve to their real @Primary domain implementations")
    void sharedContractsResolveToPrimaryDomainServices() {
        assertThat(vehicleLookupService).isInstanceOf(VehicleService.class);
        assertThat(driverLookupService).isInstanceOf(DriverService.class);
        assertThat(tripLookupService).isInstanceOf(TripService.class);
        assertThat(shipmentLookupService).isInstanceOf(ShipmentService.class);
        assertThat(alertPublisher).isInstanceOf(TrackingService.class);
    }

    @Test
    @DisplayName("Zero cross-module repository or entity imports exist across all 5 domain packages")
    void zeroCrossModuleImportsAcrossDomainPackages() throws IOException {
        List<String> modules = List.of("auth", "fleet", "drivers", "shipments", "tracking");
        Path baseDir = Paths.get("src/main/java/com/fleetx");
        List<String> violations = new ArrayList<>();

        for (String currentModule : modules) {
            Path modulePath = baseDir.resolve(currentModule);
            if (!Files.exists(modulePath)) continue;

            try (Stream<Path> paths = Files.walk(modulePath)) {
                paths.filter(p -> p.toString().endsWith(".java")).forEach(javaFile -> {
                    try {
                        List<String> lines = Files.readAllLines(javaFile);
                        for (int i = 0; i < lines.size(); i++) {
                            String line = lines.get(i).trim();
                            if (line.startsWith("import com.fleetx.")) {
                                for (String otherModule : modules) {
                                    if (!otherModule.equals(currentModule)
                                            && line.startsWith("import com.fleetx." + otherModule + ".")) {
                                        violations.add(javaFile + ":" + (i + 1) + " -> " + line);
                                    }
                                }
                            }
                        }
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
            }
        }

        assertThat(violations)
                .as("Domain modules must never import another domain module directly")
                .isEmpty();
    }
}
