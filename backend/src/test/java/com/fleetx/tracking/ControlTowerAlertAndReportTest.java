package com.fleetx.tracking;

import com.fleetx.shared.contract.AlertPublisher;
import com.fleetx.tracking.service.TrackingService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ControlTowerAlertAndReportTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AlertPublisher alertPublisher;

    @Autowired
    private TrackingService trackingService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @WithMockUser(username = "admin@fleetx.com", roles = {"ADMIN"})
    @DisplayName("1. AlertPublisher deduplicates repeated unacknowledged alerts and allows acknowledgement")
    void alertPublisherDeduplicatesAndSupportsAcknowledgement() throws Exception {
        Long entityId = 9001L;

        // Publish identical alert 3 times while unacknowledged
        alertPublisher.publishAlert("OVERSPEED", "WARNING", "VEHICLE", entityId, "Speeding 98 km/h", Instant.now());
        alertPublisher.publishAlert("OVERSPEED", "WARNING", "VEHICLE", entityId, "Speeding 101 km/h", Instant.now());
        alertPublisher.publishAlert("OVERSPEED", "WARNING", "VEHICLE", entityId, "Speeding 99 km/h", Instant.now());

        Long unackCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM alerts WHERE alert_type = 'OVERSPEED' AND entity_id = ? AND acknowledged = FALSE",
                Long.class, entityId
        );
        assertThat(unackCount).isEqualTo(1L);

        Long alertId = jdbcTemplate.queryForObject(
                "SELECT id FROM alerts WHERE alert_type = 'OVERSPEED' AND entity_id = ? AND acknowledged = FALSE",
                Long.class, entityId
        );

        // Acknowledge alert via REST endpoint
        mockMvc.perform(post("/api/v1/tracking/alerts/" + alertId + "/acknowledge")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.acknowledged").value(true))
                .andExpect(jsonPath("$.data.acknowledgedBy").value("admin@fleetx.com"));
    }

    @Test
    @WithMockUser(username = "viewer@fleetx.com", roles = {"VIEWER"})
    @DisplayName("2. Control Tower summary returns real persisted aggregates and explicit SIMULATED DATA disclaimer")
    void controlTowerSummaryReturnsRealAggregates() throws Exception {
        mockMvc.perform(get("/api/v1/tracking/control-tower/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.simulatedData").value(true))
                .andExpect(jsonPath("$.data.simulatedDataDisclaimer", containsString("SIMULATED DATA")))
                .andExpect(jsonPath("$.data.totalVehicles", greaterThanOrEqualTo(0)))
                .andExpect(jsonPath("$.data.activeAlertsCount", greaterThanOrEqualTo(0)));
    }

    @Test
    @DisplayName("3. CSV escaping adheres to RFC 4180 and neutralizes spreadsheet formula injection")
    void csvEscapingHandlesCommasQuotesNewlinesAndFormulas() {
        assertThat(trackingService.escapeCsvField("Pune, Maharashtra")).isEqualTo("\"Pune, Maharashtra\"");
        assertThat(trackingService.escapeCsvField("Bay \"A\" North")).isEqualTo("\"Bay \"\"A\"\" North\"");
        assertThat(trackingService.escapeCsvField("Line 1\nLine 2")).isEqualTo("\"Line 1\nLine 2\"");
        assertThat(trackingService.escapeCsvField("=CMD|' /C calc'!A0")).isEqualTo("\"'=CMD|' /C calc'!A0\"");
        assertThat(trackingService.escapeCsvField("@SUM(A1:A10)")).isEqualTo("\"'@SUM(A1:A10)\"");
        // Negative numbers should not be prefixed with a single quote
        assertThat(trackingService.escapeCsvField(-42.5)).isEqualTo("-42.5");
    }

    @Test
    @WithMockUser(username = "viewer@fleetx.com", roles = {"VIEWER"})
    @DisplayName("4. Authorized VIEWER can export RFC 4180 CSV reports")
    void viewerCanExportCsvReport() throws Exception {
        alertPublisher.publishAlert(
                "SHIPMENT_DELAYED",
                "WARNING",
                "SHIPMENT",
                701L,
                "Shipment delayed at \"Lonavala, Ghat\" checkpoint",
                Instant.now()
        );

        mockMvc.perform(get("/api/v1/tracking/reports/export-csv")
                        .param("reportType", "ALERTS"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", containsString("fleetx_alerts_report.csv")))
                .andExpect(content().string(containsString("alert_id,alert_type,severity,entity_type,entity_id,acknowledged,message,created_at")))
                .andExpect(content().string(containsString("\"Shipment delayed at \"\"Lonavala, Ghat\"\" checkpoint\"")));
    }

    @Test
    @WithMockUser(username = "driver@fleetx.com", roles = {"DRIVER"})
    @DisplayName("5. Role permissions block DRIVER from starting simulator, acknowledging alerts, or exporting CSV reports")
    void driverRoleIsForbiddenFromPrivilegedControlTowerActions() throws Exception {
        mockMvc.perform(post("/api/v1/tracking/simulator/start")
                        .with(csrf())
                        .contentType("application/json")
                        .content("{\"vehicleId\": 1}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/v1/tracking/alerts/1/acknowledge")
                        .with(csrf()))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/tracking/reports/export-csv")
                        .param("reportType", "TRIPS"))
                .andExpect(status().isForbidden());
    }
}
