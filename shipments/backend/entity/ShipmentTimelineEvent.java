package com.fleetx.shipments.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "shipment_timeline_events")
public class ShipmentTimelineEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "shipment_id", nullable = false)
    private Long shipmentId;

    @Column(name = "event_type", nullable = false, length = 50)
    private String eventType;

    @Column(name = "location_summary", length = 255)
    private String locationSummary;

    @Column(name = "remarks", columnDefinition = "TEXT")
    private String remarks;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public ShipmentTimelineEvent() {}

    public ShipmentTimelineEvent(Long shipmentId, String eventType, String locationSummary, String remarks) {
        this.shipmentId = shipmentId;
        this.eventType = eventType;
        this.locationSummary = locationSummary;
        this.remarks = remarks;
        this.createdAt = Instant.now();
    }

    @PrePersist
    public void onPrePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getShipmentId() { return shipmentId; }
    public void setShipmentId(Long shipmentId) { this.shipmentId = shipmentId; }

    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }

    public String getLocationSummary() { return locationSummary; }
    public void setLocationSummary(String locationSummary) { this.locationSummary = locationSummary; }

    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
