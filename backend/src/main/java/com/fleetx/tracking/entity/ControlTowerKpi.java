package com.fleetx.tracking.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "control_tower_kpis")
public class ControlTowerKpi {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "snapshot_date", nullable = false)
    private LocalDate snapshotDate;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "active_vehicles", nullable = false)
    private Integer activeVehicles = 0;

    @Column(name = "ongoing_trips", nullable = false)
    private Integer ongoingTrips = 0;

    @Column(name = "total_alerts", nullable = false)
    private Integer totalAlerts = 0;

    @JdbcTypeCode(SqlTypes.DECIMAL)
    @Column(name = "average_fuel_efficiency", nullable = false)
    private Double averageFuelEfficiency = 0.0;

    @Column(name = "generated_at", nullable = false)
    private Instant generatedAt = Instant.now();

    public ControlTowerKpi() {}

    @PrePersist
    public void onPrePersist() {
        if (generatedAt == null) {
            generatedAt = Instant.now();
        }
        if (tenantId == null) {
            tenantId = com.fleetx.shared.context.TenantSecurityContext.getOptionalTenantId().orElse(1L);
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public LocalDate getSnapshotDate() { return snapshotDate; }
    public void setSnapshotDate(LocalDate snapshotDate) { this.snapshotDate = snapshotDate; }

    public Integer getActiveVehicles() { return activeVehicles; }
    public void setActiveVehicles(Integer activeVehicles) { this.activeVehicles = activeVehicles; }

    public Integer getOngoingTrips() { return ongoingTrips; }
    public void setOngoingTrips(Integer ongoingTrips) { this.ongoingTrips = ongoingTrips; }

    public Integer getTotalAlerts() { return totalAlerts; }
    public void setTotalAlerts(Integer totalAlerts) { this.totalAlerts = totalAlerts; }

    public Double getAverageFuelEfficiency() { return averageFuelEfficiency; }
    public void setAverageFuelEfficiency(Double averageFuelEfficiency) { this.averageFuelEfficiency = averageFuelEfficiency; }

    public Instant getGeneratedAt() { return generatedAt; }
    public void setGeneratedAt(Instant generatedAt) { this.generatedAt = generatedAt; }

    public Long getTenantId() { return tenantId; }
    public void setTenantId(Long tenantId) { this.tenantId = tenantId; }
}
