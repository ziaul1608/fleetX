package com.fleetx.tracking.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "geofence_vehicle_states")
public class GeofenceVehicleState {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "geofence_id", nullable = false)
    private Long geofenceId;

    @Column(name = "vehicle_id", nullable = false)
    private Long vehicleId;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId = 1L;

    @Column(nullable = false)
    private Boolean inside;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public GeofenceVehicleState() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getGeofenceId() { return geofenceId; }
    public void setGeofenceId(Long geofenceId) { this.geofenceId = geofenceId; }

    public Long getVehicleId() { return vehicleId; }
    public void setVehicleId(Long vehicleId) { this.vehicleId = vehicleId; }

    public Boolean getInside() { return inside; }
    public void setInside(Boolean inside) { this.inside = inside; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public Long getTenantId() { return tenantId; }
    public void setTenantId(Long tenantId) { this.tenantId = tenantId; }
}
