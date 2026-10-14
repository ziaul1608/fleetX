package com.fleetx.shipments.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "shipments")
public class Shipment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tracking_number", nullable = false, unique = true, length = 30)
    private String trackingNumber;

    @Column(name = "trip_id")
    private Long tripId;

    @Column(name = "sender_name", nullable = false, length = 120)
    private String senderName;

    @Column(name = "sender_address", nullable = false, length = 255)
    private String senderAddress = "Origin Dispatch Hub";

    @Column(name = "receiver_name", nullable = false, length = 120)
    private String receiverName;

    @Column(name = "receiver_address", nullable = false, length = 255)
    private String receiverAddress = "Destination Delivery Hub";

    @Column(name = "cargo_description", columnDefinition = "TEXT")
    private String cargoDescription;

    @JdbcTypeCode(SqlTypes.DECIMAL)
    @Column(name = "weight_kg", nullable = false)
    private Double weightKg;

    @JdbcTypeCode(SqlTypes.DECIMAL)
    @Column(name = "volume_m3", nullable = false)
    private Double volumeM3;

    @Column(name = "status", nullable = false, length = 30)
    private String status = "CREATED";

    @Column(name = "promised_delivery_time")
    private Instant promisedDeliveryTime;

    @Column(name = "estimated_distance_km", precision = 10, scale = 2)
    private BigDecimal estimatedDistanceKm;

    @Column(name = "estimated_duration_minutes")
    private Integer estimatedDurationMinutes;

    @Column(name = "estimated_arrival_time")
    private Instant estimatedArrivalTime;

    @Column(name = "delivered_at")
    private Instant deliveredAt;

    @Column(name = "delivery_notes", columnDefinition = "TEXT")
    private String deliveryNotes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public Shipment() {}

    @PrePersist
    public void onPrePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        updatedAt = Instant.now();
        if (status == null) {
            status = "CREATED";
        }
        if (senderAddress == null) {
            senderAddress = "Origin Dispatch Hub";
        }
        if (receiverAddress == null) {
            receiverAddress = "Destination Delivery Hub";
        }
    }

    @PreUpdate
    public void onPreUpdate() {
        updatedAt = Instant.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTrackingNumber() { return trackingNumber; }
    public void setTrackingNumber(String trackingNumber) { this.trackingNumber = trackingNumber != null ? trackingNumber.trim().toUpperCase() : null; }

    public Long getTripId() { return tripId; }
    public void setTripId(Long tripId) { this.tripId = tripId; }

    public String getSenderName() { return senderName; }
    public void setSenderName(String senderName) { this.senderName = senderName; }

    public String getSenderAddress() { return senderAddress; }
    public void setSenderAddress(String senderAddress) { this.senderAddress = senderAddress; }

    public String getReceiverName() { return receiverName; }
    public void setReceiverName(String receiverName) { this.receiverName = receiverName; }

    public String getReceiverAddress() { return receiverAddress; }
    public void setReceiverAddress(String receiverAddress) { this.receiverAddress = receiverAddress; }

    public String getCargoDescription() { return cargoDescription; }
    public void setCargoDescription(String cargoDescription) { this.cargoDescription = cargoDescription; }

    public Double getWeightKg() { return weightKg; }
    public void setWeightKg(Double weightKg) { this.weightKg = weightKg; }

    public Double getVolumeM3() { return volumeM3; }
    public void setVolumeM3(Double volumeM3) { this.volumeM3 = volumeM3; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status != null ? status.trim().toUpperCase() : "CREATED"; }

    public Instant getPromisedDeliveryTime() { return promisedDeliveryTime; }
    public void setPromisedDeliveryTime(Instant promisedDeliveryTime) { this.promisedDeliveryTime = promisedDeliveryTime; }

    public BigDecimal getEstimatedDistanceKm() { return estimatedDistanceKm; }
    public void setEstimatedDistanceKm(BigDecimal estimatedDistanceKm) { this.estimatedDistanceKm = estimatedDistanceKm; }

    public Integer getEstimatedDurationMinutes() { return estimatedDurationMinutes; }
    public void setEstimatedDurationMinutes(Integer estimatedDurationMinutes) { this.estimatedDurationMinutes = estimatedDurationMinutes; }

    public Instant getEstimatedArrivalTime() { return estimatedArrivalTime; }
    public void setEstimatedArrivalTime(Instant estimatedArrivalTime) { this.estimatedArrivalTime = estimatedArrivalTime; }

    public Instant getDeliveredAt() { return deliveredAt; }
    public void setDeliveredAt(Instant deliveredAt) { this.deliveredAt = deliveredAt; }

    public String getDeliveryNotes() { return deliveryNotes; }
    public void setDeliveryNotes(String deliveryNotes) { this.deliveryNotes = deliveryNotes; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
