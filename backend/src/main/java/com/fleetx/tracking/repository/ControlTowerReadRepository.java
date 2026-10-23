package com.fleetx.tracking.repository;

import com.fleetx.shared.context.TenantSecurityContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.*;

/**
 * Read-only SQL projection repository for the Control Tower and CSV reporting engine.
 * Complies with Rule 1.2 (zero imports from other domain modules' JPA repositories or entities)
 * while querying actual persisted relational data across the modular monolith database.
 */
@Repository
public class ControlTowerReadRepository {

    private final JdbcTemplate jdbcTemplate;

    public ControlTowerReadRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private Long currentTenant() {
        return TenantSecurityContext.getOptionalTenantId().orElse(1L);
    }

    public Map<String, Long> countVehiclesByStatus() {
        Map<String, Long> result = new LinkedHashMap<>();
        jdbcTemplate.query(
                "SELECT UPPER(status) AS st, COUNT(*) AS cnt FROM vehicles WHERE active = TRUE AND tenant_id = ? GROUP BY UPPER(status)",
                rs -> {
                    result.put(rs.getString("st"), rs.getLong("cnt"));
                },
                currentTenant()
        );
        return result;
    }

    public long countTotalVehicles() {
        Long cnt = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM vehicles WHERE tenant_id = ?", Long.class, currentTenant());
        return cnt != null ? cnt : 0L;
    }

    public long countInactiveVehicles() {
        Long cnt = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM vehicles WHERE active = FALSE AND tenant_id = ?", Long.class, currentTenant());
        return cnt != null ? cnt : 0L;
    }

    public Map<String, Long> countTripsByStatus() {
        Map<String, Long> result = new LinkedHashMap<>();
        jdbcTemplate.query(
                "SELECT UPPER(status) AS st, COUNT(*) AS cnt FROM trips WHERE tenant_id = ? GROUP BY UPPER(status)",
                rs -> {
                    result.put(rs.getString("st"), rs.getLong("cnt"));
                },
                currentTenant()
        );
        return result;
    }

    public long countTripsCompletedSince(Instant since) {
        Long cnt = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM trips WHERE UPPER(status) = 'COMPLETED' AND (actual_end_time >= ? OR created_at >= ?) AND tenant_id = ?",
                Long.class,
                Timestamp.from(since),
                Timestamp.from(since),
                currentTenant()
        );
        return cnt != null ? cnt : 0L;
    }

    public Map<String, Long> countShipmentsByStatus() {
        Map<String, Long> result = new LinkedHashMap<>();
        jdbcTemplate.query(
                "SELECT UPPER(status) AS st, COUNT(*) AS cnt FROM shipments WHERE tenant_id = ? GROUP BY UPPER(status)",
                rs -> {
                    result.put(rs.getString("st"), rs.getLong("cnt"));
                },
                currentTenant()
        );
        return result;
    }

    public Double getAverageFuelEfficiency() {
        Double avg = jdbcTemplate.queryForObject(
                "SELECT AVG(efficiency_km_per_liter) FROM fuel_records WHERE efficiency_km_per_liter IS NOT NULL AND efficiency_km_per_liter > 0 AND tenant_id = ?",
                Double.class,
                currentTenant()
        );
        return avg != null ? Math.round(avg * 100.0) / 100.0 : 0.0;
    }

    public Double getTotalFuelCost() {
        Double sum = jdbcTemplate.queryForObject(
                "SELECT COALESCE(SUM(total_cost), 0) FROM fuel_records WHERE tenant_id = ?",
                Double.class,
                currentTenant()
        );
        return sum != null ? Math.round(sum * 100.0) / 100.0 : 0.0;
    }

    public Double getTotalMaintenanceCost() {
        Double sum = jdbcTemplate.queryForObject(
                "SELECT COALESCE(SUM(cost), 0) FROM maintenance_records WHERE tenant_id = ?",
                Double.class,
                currentTenant()
        );
        return sum != null ? Math.round(sum * 100.0) / 100.0 : 0.0;
    }

    public long countUnresolvedMaintenanceWarnings() {
        Long cnt = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM service_warnings WHERE resolved = FALSE AND tenant_id = ?",
                Long.class,
                currentTenant()
        );
        return cnt != null ? cnt : 0L;
    }

    public List<Map<String, Object>> findWaypointsForTrip(Long tripId) {
        if (tripId == null) {
            return Collections.emptyList();
        }
        String sql = """
                SELECT sw.waypoint_name, sw.latitude, sw.longitude, sw.sequence_order, sw.stop_type
                FROM shipment_waypoints sw
                INNER JOIN shipments s ON sw.shipment_id = s.id
                WHERE s.trip_id = ? AND s.tenant_id = ?
                ORDER BY s.id ASC, sw.sequence_order ASC
                """;
        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("label", rs.getString("waypoint_name"));
            map.put("latitude", rs.getDouble("latitude"));
            map.put("longitude", rs.getDouble("longitude"));
            map.put("sequenceOrder", rs.getInt("sequence_order"));
            map.put("stopType", rs.getString("stop_type"));
            return map;
        }, tripId, currentTenant());
    }

    public Optional<Long> findActiveTripIdForVehicle(Long vehicleId) {
        if (vehicleId == null) {
            return Optional.empty();
        }
        List<Long> ids = jdbcTemplate.query(
                "SELECT id FROM trips WHERE vehicle_id = ? AND tenant_id = ? AND UPPER(status) IN ('IN_PROGRESS', 'IN_TRANSIT', 'ASSIGNED', 'PLANNED') ORDER BY id DESC",
                (rs, rowNum) -> rs.getLong("id"),
                vehicleId,
                currentTenant()
        );
        return ids.stream().findFirst();
    }

    public List<Map<String, Object>> findAllVehiclesOverview() {
        String sql = """
                SELECT v.id, v.license_plate, v.make, v.model, v.vehicle_type, v.status, v.odometer_km, v.active
                FROM vehicles v
                WHERE v.tenant_id = ?
                ORDER BY v.id ASC
                """;
        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("vehicleId", rs.getLong("id"));
            m.put("licensePlate", rs.getString("license_plate"));
            m.put("make", rs.getString("make"));
            m.put("model", rs.getString("model"));
            m.put("vehicleType", rs.getString("vehicle_type"));
            m.put("status", rs.getString("status"));
            m.put("odometerKm", rs.getDouble("odometer_km"));
            m.put("active", rs.getBoolean("active"));
            return m;
        }, currentTenant());
    }

    // --- Report Queries with Optional Date & Status Filtering ---

    public List<Map<String, Object>> queryTripsReport(LocalDate startDate, LocalDate endDate, String status) {
        StringBuilder sql = new StringBuilder("""
                SELECT t.trip_code, v.license_plate, d.full_name AS driver_name,
                       t.origin_address, t.destination_address, t.status,
                       COALESCE(t.distance_km, 0.0) AS distance_km, t.scheduled_start_time, t.created_at
                FROM trips t
                LEFT JOIN vehicles v ON t.vehicle_id = v.id
                LEFT JOIN drivers d ON t.driver_id = d.id
                WHERE t.tenant_id = ?
                """);
        List<Object> params = new ArrayList<>();
        params.add(currentTenant());
        appendDateRange(sql, params, "t.created_at", startDate, endDate);
        if (status != null && !status.isBlank()) {
            sql.append(" AND UPPER(t.status) = UPPER(?)");
            params.add(status.trim());
        }
        sql.append(" ORDER BY t.created_at DESC, t.id DESC");

        return jdbcTemplate.query(sql.toString(), (rs, rowNum) -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("trip_code", rs.getString("trip_code"));
            row.put("vehicle_plate", rs.getString("license_plate"));
            row.put("driver_name", rs.getString("driver_name"));
            row.put("origin", rs.getString("origin_address"));
            row.put("destination", rs.getString("destination_address"));
            row.put("status", rs.getString("status"));
            row.put("distance_km", rs.getDouble("distance_km"));
            Timestamp sched = rs.getTimestamp("scheduled_start_time");
            row.put("scheduled_start", sched != null ? sched.toInstant().toString() : "");
            return row;
        }, params.toArray());
    }

    public List<Map<String, Object>> queryShipmentsReport(LocalDate startDate, LocalDate endDate, String status) {
        StringBuilder sql = new StringBuilder("""
                SELECT s.tracking_number, s.sender_name, s.receiver_name,
                       s.sender_address AS origin_city, s.receiver_address AS destination_city, s.weight_kg, s.status,
                       COALESCE(s.estimated_distance_km, 0.0) AS estimated_distance_km, s.created_at
                FROM shipments s
                WHERE s.tenant_id = ?
                """);
        List<Object> params = new ArrayList<>();
        params.add(currentTenant());
        appendDateRange(sql, params, "s.created_at", startDate, endDate);
        if (status != null && !status.isBlank()) {
            sql.append(" AND UPPER(s.status) = UPPER(?)");
            params.add(status.trim());
        }
        sql.append(" ORDER BY s.created_at DESC, s.id DESC");

        return jdbcTemplate.query(sql.toString(), (rs, rowNum) -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("tracking_number", rs.getString("tracking_number"));
            row.put("sender_name", rs.getString("sender_name"));
            row.put("receiver_name", rs.getString("receiver_name"));
            row.put("origin_city", rs.getString("origin_city"));
            row.put("destination_city", rs.getString("destination_city"));
            row.put("weight_kg", rs.getDouble("weight_kg"));
            row.put("status", rs.getString("status"));
            row.put("estimated_distance_km", rs.getDouble("estimated_distance_km"));
            Timestamp created = rs.getTimestamp("created_at");
            row.put("created_at", created != null ? created.toInstant().toString() : "");
            return row;
        }, params.toArray());
    }

    public List<Map<String, Object>> queryFuelReport(LocalDate startDate, LocalDate endDate, String status) {
        StringBuilder sql = new StringBuilder("""
                SELECT f.id, v.license_plate, v.status AS vehicle_status,
                       f.liters, f.total_cost, f.odometer_at_fueling,
                       f.efficiency_km_per_liter, f.fuel_station, f.fuel_date
                FROM fuel_records f
                LEFT JOIN vehicles v ON f.vehicle_id = v.id
                WHERE f.tenant_id = ?
                """);
        List<Object> params = new ArrayList<>();
        params.add(currentTenant());
        appendDateRange(sql, params, "f.fuel_date", startDate, endDate);
        if (status != null && !status.isBlank()) {
            sql.append(" AND UPPER(v.status) = UPPER(?)");
            params.add(status.trim());
        }
        sql.append(" ORDER BY f.fuel_date DESC, f.id DESC");

        return jdbcTemplate.query(sql.toString(), (rs, rowNum) -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("record_id", rs.getLong("id"));
            row.put("vehicle_plate", rs.getString("license_plate"));
            row.put("liters", rs.getDouble("liters"));
            row.put("total_cost", rs.getDouble("total_cost"));
            row.put("odometer_km", rs.getDouble("odometer_at_fueling"));
            Object eff = rs.getObject("efficiency_km_per_liter");
            row.put("efficiency_km_l", eff != null ? rs.getDouble("efficiency_km_per_liter") : "");
            row.put("fuel_station", rs.getString("fuel_station") != null ? rs.getString("fuel_station") : "");
            Timestamp dt = rs.getTimestamp("fuel_date");
            row.put("fuel_date", dt != null ? dt.toInstant().toString() : "");
            return row;
        }, params.toArray());
    }

    public List<Map<String, Object>> queryMaintenanceReport(LocalDate startDate, LocalDate endDate, String status) {
        StringBuilder sql = new StringBuilder("""
                SELECT m.id, v.license_plate, m.service_type, m.status,
                       m.cost, m.odometer_at_service, m.service_date, m.description
                FROM maintenance_records m
                LEFT JOIN vehicles v ON m.vehicle_id = v.id
                WHERE m.tenant_id = ?
                """);
        List<Object> params = new ArrayList<>();
        params.add(currentTenant());
        if (startDate != null) {
            sql.append(" AND m.service_date >= ?");
            params.add(java.sql.Date.valueOf(startDate));
        }
        if (endDate != null) {
            sql.append(" AND m.service_date <= ?");
            params.add(java.sql.Date.valueOf(endDate));
        }
        if (status != null && !status.isBlank()) {
            sql.append(" AND UPPER(m.status) = UPPER(?)");
            params.add(status.trim());
        }
        sql.append(" ORDER BY m.service_date DESC, m.id DESC");

        return jdbcTemplate.query(sql.toString(), (rs, rowNum) -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("record_id", rs.getLong("id"));
            row.put("vehicle_plate", rs.getString("license_plate"));
            row.put("service_type", rs.getString("service_type"));
            row.put("status", rs.getString("status"));
            row.put("cost", rs.getDouble("cost"));
            row.put("odometer_km", rs.getDouble("odometer_at_service"));
            java.sql.Date sd = rs.getDate("service_date");
            row.put("service_date", sd != null ? sd.toString() : "");
            row.put("description", rs.getString("description"));
            return row;
        }, params.toArray());
    }

    public List<Map<String, Object>> queryAlertsReport(LocalDate startDate, LocalDate endDate, String status) {
        StringBuilder sql = new StringBuilder("""
                SELECT a.id, a.alert_type, a.severity, a.entity_type, a.entity_id,
                       a.message, a.acknowledged, a.created_at
                FROM alerts a
                WHERE a.tenant_id = ?
                """);
        List<Object> params = new ArrayList<>();
        params.add(currentTenant());
        appendDateRange(sql, params, "a.created_at", startDate, endDate);
        if (status != null && !status.isBlank()) {
            if ("ACKNOWLEDGED".equalsIgnoreCase(status.trim())) {
                sql.append(" AND a.acknowledged = TRUE");
            } else if ("UNACKNOWLEDGED".equalsIgnoreCase(status.trim()) || "OPEN".equalsIgnoreCase(status.trim())) {
                sql.append(" AND a.acknowledged = FALSE");
            } else {
                sql.append(" AND (UPPER(a.severity) = UPPER(?) OR UPPER(a.alert_type) = UPPER(?))");
                params.add(status.trim());
                params.add(status.trim());
            }
        }
        sql.append(" ORDER BY a.created_at DESC, a.id DESC");

        return jdbcTemplate.query(sql.toString(), (rs, rowNum) -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("alert_id", rs.getLong("id"));
            row.put("alert_type", rs.getString("alert_type"));
            row.put("severity", rs.getString("severity"));
            row.put("entity_type", rs.getString("entity_type"));
            row.put("entity_id", rs.getLong("entity_id"));
            row.put("acknowledged", rs.getBoolean("acknowledged"));
            row.put("message", rs.getString("message"));
            Timestamp created = rs.getTimestamp("created_at");
            row.put("created_at", created != null ? created.toInstant().toString() : "");
            return row;
        }, params.toArray());
    }

    public List<Map<String, Object>> queryVehiclesReport(LocalDate startDate, LocalDate endDate, String status) {
        StringBuilder sql = new StringBuilder("""
                SELECT v.id, v.vin, v.license_plate, v.make, v.model, v.model_year,
                       v.vehicle_type, v.fuel_type, v.odometer_km, v.status, v.created_at
                FROM vehicles v
                WHERE v.tenant_id = ?
                """);
        List<Object> params = new ArrayList<>();
        params.add(currentTenant());
        appendDateRange(sql, params, "v.created_at", startDate, endDate);
        if (status != null && !status.isBlank()) {
            sql.append(" AND UPPER(v.status) = UPPER(?)");
            params.add(status.trim());
        }
        sql.append(" ORDER BY v.id ASC");

        return jdbcTemplate.query(sql.toString(), (rs, rowNum) -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("vehicle_id", rs.getLong("id"));
            row.put("vin", rs.getString("vin"));
            row.put("license_plate", rs.getString("license_plate"));
            row.put("make", rs.getString("make"));
            row.put("model", rs.getString("model"));
            row.put("year", rs.getInt("model_year"));
            row.put("type", rs.getString("vehicle_type"));
            row.put("fuel_type", rs.getString("fuel_type"));
            row.put("odometer_km", rs.getDouble("odometer_km"));
            row.put("status", rs.getString("status"));
            return row;
        }, params.toArray());
    }

    private void appendDateRange(StringBuilder sql, List<Object> params, String column, LocalDate startDate, LocalDate endDate) {
        if (startDate != null) {
            Instant startInstant = startDate.atStartOfDay(ZoneOffset.UTC).toInstant();
            sql.append(" AND ").append(column).append(" >= ?");
            params.add(Timestamp.from(startInstant));
        }
        if (endDate != null) {
            Instant endInstant = endDate.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
            sql.append(" AND ").append(column).append(" < ?");
            params.add(Timestamp.from(endInstant));
        }
    }
}
