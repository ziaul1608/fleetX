package com.fleetx.fleet.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "service_warnings")
public class ServiceWarning {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id", nullable = false)
    private Vehicle vehicle;

    @Column(name = "warning_type", nullable = false, length = 50)
    private String warningType;

    @Column(nullable = false, length = 20)
    private String severity = "WARNING";

    @Column(nullable = false, length = 255)
    private String message;

    @Column(nullable = false)
    private Boolean resolved = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public ServiceWarning() {}

    public ServiceWarning(Vehicle vehicle, String warningType, String severity, String message) {
        this.vehicle = vehicle;
        this.warningType = warningType;
        this.severity = severity != null ? severity : "WARNING";
        this.message = message;
        this.resolved = false;
        this.createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Vehicle getVehicle() { return vehicle; }
    public void setVehicle(Vehicle vehicle) { this.vehicle = vehicle; }

    public String getWarningType() { return warningType; }
    public void setWarningType(String warningType) { this.warningType = warningType; }

    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public Boolean getResolved() { return resolved; }
    public void setResolved(Boolean resolved) { this.resolved = resolved; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
