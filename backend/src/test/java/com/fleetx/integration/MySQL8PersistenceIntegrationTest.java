package com.fleetx.integration;

import com.fleetx.fleet.dto.VehicleCreateRequest;
import com.fleetx.fleet.dto.VehicleResponseDto;
import com.fleetx.fleet.service.VehicleService;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.sql.DataSource;
import java.net.Socket;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@org.springframework.test.context.ActiveProfiles("qa")
@EnabledIf("isMySQLAvailable")
class MySQL8PersistenceIntegrationTest {


    static boolean isMySQLAvailable() {
        try (Socket socket = new Socket("127.0.0.1", 3307)) {
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Autowired
    private DataSource dataSource;

    @Autowired
    private Flyway flyway;

    @Autowired
    private VehicleService vehicleService;

    @Test
    @DisplayName("Verify real MySQL 8.4 database connection metadata and version")
    void testRealMySQLDatabaseMetadata() throws Exception {
        try (Connection conn = dataSource.getConnection()) {
            DatabaseMetaData metaData = conn.getMetaData();
            String productName = metaData.getDatabaseProductName();
            String productVersion = metaData.getDatabaseProductVersion();

            assertThat(productName).isEqualTo("MySQL");
            assertThat(productVersion).contains("8.4");
        }
    }

    @Test
    @DisplayName("Verify all 12 Flyway migrations are recorded and active on MySQL 8.4 schema")
    void testFlywayMigrationsOnMySQL() {
        MigrationInfo[] applied = flyway.info().applied();
        assertThat(applied).hasSize(12);

        List<String> versions = Arrays.stream(applied)
                .map(info -> info.getVersion().getVersion())
                .toList();
        assertThat(versions).containsExactly(
                "100", "101",
                "200", "201",
                "300", "301",
                "400", "401",
                "500", "501",
                "600", "601"
        );
    }

    @Test
    @DisplayName("Verify MySQL 8.4 table schema existence and foreign keys")
    void testTableExistenceAndSchema() throws Exception {
        List<String> expectedTables = List.of(
                "tenants", "otp_challenges",
                "users", "roles", "user_roles", "audit_logs",
                "vehicles", "fuel_records", "maintenance_records", "service_warnings",
                "drivers", "trips", "trip_status_history", "driver_performance_metrics",
                "shipments", "shipment_waypoints", "shipment_timeline_events",
                "telemetry_logs", "geofences", "alerts", "control_tower_kpis",
                "geofence_vehicle_states", "flyway_schema_history"
        );

        List<String> actualTables = new ArrayList<>();
        try (Connection conn = dataSource.getConnection()) {
            ResultSet rs = conn.getMetaData().getTables("fleetx_qa", null, "%", new String[]{"TABLE"});
            while (rs.next()) {
                actualTables.add(rs.getString("TABLE_NAME").toLowerCase());
            }
        }

        assertThat(actualTables).containsAll(expectedTables);
    }

    @Test
    @DisplayName("Verify vehicle creation and persistence in MySQL 8.4")
    void testVehiclePersistenceInMySQL() {
        long suffix = System.currentTimeMillis() % 100000;
        String testPlate = "KA-05-IT-" + (suffix < 1000 ? suffix + 1000 : suffix);
        String testVin = "1HGCR2F83HA" + String.format("%06d", suffix);

        VehicleCreateRequest request = new VehicleCreateRequest(
                testVin,
                testPlate,
                "Tata Motors",
                "Prima 5530.S",
                2026,
                "TRUCK",
                "DIESEL",
                40000.0,
                110.0,
                0.0,
                "AVAILABLE"
        );

        VehicleResponseDto created = vehicleService.createVehicle(request);
        assertThat(created.id()).isNotNull();
        assertThat(created.licensePlate()).isEqualTo(testPlate);

        VehicleResponseDto fetched = vehicleService.getVehicleById(created.id());
        assertThat(fetched.vin()).isEqualTo(testVin);
        assertThat(fetched.status()).isEqualTo("AVAILABLE");
    }
}

