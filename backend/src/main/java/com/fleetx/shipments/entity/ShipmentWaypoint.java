package com.fleetx.shipments.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "shipment_waypoints")
public class ShipmentWaypoint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "shipment_id", nullable = false)
    private Long shipmentId;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private Long tenantId;

    @Column(name = "waypoint_name", nullable = false, length = 120)
    private String waypointName;

    @Column(name = "latitude", nullable = false, precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(name = "longitude", nullable = false, precision = 10, scale = 7)
    private BigDecimal longitude;

    @Column(name = "sequence_order", nullable = false)
    private Integer sequenceOrder;

    @Column(name = "stop_type", nullable = false, length = 30)
    private String stopType = "INTERMEDIATE";

    @Column(name = "estimated_arrival")
    private Instant estimatedArrival;

    public ShipmentWaypoint() {}

    public ShipmentWaypoint(Long shipmentId, String waypointName, BigDecimal latitude, BigDecimal longitude,
                            Integer sequenceOrder, String stopType, Instant estimatedArrival) {
        this.shipmentId = shipmentId;
        this.waypointName = waypointName;
        this.latitude = latitude;
        this.longitude = longitude;
        this.sequenceOrder = sequenceOrder;
        this.stopType = stopType != null ? stopType.trim().toUpperCase() : "INTERMEDIATE";
        this.estimatedArrival = estimatedArrival;
    }

    @PrePersist
    public void onPrePersist() {
        this.tenantId = com.fleetx.shared.context.TenantSecurityContext.validateAndAssignTenantForPersist(this.tenantId, "ShipmentWaypoint");
    }

    @PreUpdate
    public void onPreUpdate() {
        com.fleetx.shared.context.TenantSecurityContext.validateTenantForUpdate(this.tenantId, "ShipmentWaypoint");
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getShipmentId() { return shipmentId; }
    public void setShipmentId(Long shipmentId) { this.shipmentId = shipmentId; }

    public String getWaypointName() { return waypointName; }
    public void setWaypointName(String waypointName) { this.waypointName = waypointName; }

    public BigDecimal getLatitude() { return latitude; }
    public void setLatitude(BigDecimal latitude) { this.latitude = latitude; }

    public BigDecimal getLongitude() { return longitude; }
    public void setLongitude(BigDecimal longitude) { this.longitude = longitude; }

    public Integer getSequenceOrder() { return sequenceOrder; }
    public void setSequenceOrder(Integer sequenceOrder) { this.sequenceOrder = sequenceOrder; }

    public String getStopType() { return stopType; }
    public void setStopType(String stopType) { this.stopType = stopType != null ? stopType.trim().toUpperCase() : "INTERMEDIATE"; }

    public Instant getEstimatedArrival() { return estimatedArrival; }
    public void setEstimatedArrival(Instant estimatedArrival) { this.estimatedArrival = estimatedArrival; }

    public Long getTenantId() { return tenantId; }
    public void setTenantId(Long tenantId) { this.tenantId = tenantId; }
}
