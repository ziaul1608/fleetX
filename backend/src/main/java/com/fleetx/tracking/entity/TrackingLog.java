package com.fleetx.tracking.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.Instant;

@Entity
@Table(name = "telemetry_logs")
public class TrackingLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "vehicle_id", nullable = false)
    private Long vehicleId;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId = 1L;

    @Column(name = "trip_id")
    private Long tripId;

    @JdbcTypeCode(SqlTypes.DECIMAL)
    @Column(nullable = false)
    private Double latitude;

    @JdbcTypeCode(SqlTypes.DECIMAL)
    @Column(nullable = false)
    private Double longitude;

    @JdbcTypeCode(SqlTypes.DECIMAL)
    @Column(name = "speed_kmh", nullable = false)
    private Double speedKmh = 0.0;

    @JdbcTypeCode(SqlTypes.DECIMAL)
    @Column(nullable = false)
    private Double heading = 0.0;

    @JdbcTypeCode(SqlTypes.DECIMAL)
    @Column(name = "current_odometer_km", nullable = false)
    private Double currentOdometerKm = 0.0;

    @Column(name = "simulated", nullable = false)
    private Boolean simulated = true;

    @Column(name = "waypoint_label", length = 120)
    private String waypointLabel;

    @Column(name = "recorded_at", nullable = false)
    private Instant recordedAt = Instant.now();

    public TrackingLog() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getVehicleId() { return vehicleId; }
    public void setVehicleId(Long vehicleId) { this.vehicleId = vehicleId; }

    public Long getTripId() { return tripId; }
    public void setTripId(Long tripId) { this.tripId = tripId; }

    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }

    public Double getSpeedKmh() { return speedKmh; }
    public void setSpeedKmh(Double speedKmh) { this.speedKmh = speedKmh; }

    public Double getHeading() { return heading; }
    public void setHeading(Double heading) { this.heading = heading; }

    public Double getCurrentOdometerKm() { return currentOdometerKm; }
    public void setCurrentOdometerKm(Double currentOdometerKm) { this.currentOdometerKm = currentOdometerKm; }

    public Boolean getSimulated() { return simulated; }
    public void setSimulated(Boolean simulated) { this.simulated = simulated; }

    public String getWaypointLabel() { return waypointLabel; }
    public void setWaypointLabel(String waypointLabel) { this.waypointLabel = waypointLabel; }

    public Instant getRecordedAt() { return recordedAt; }
    public void setRecordedAt(Instant recordedAt) { this.recordedAt = recordedAt; }

    public Long getTenantId() { return tenantId; }
    public void setTenantId(Long tenantId) { this.tenantId = tenantId; }
}
