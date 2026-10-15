package com.fleetx.fleet.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "maintenance_records")
public class MaintenanceRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id", nullable = false)
    private Vehicle vehicle;

    @Column(name = "service_type", nullable = false, length = 40)
    private String serviceType;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal cost;

    @Column(name = "service_date", nullable = false)
    private LocalDate serviceDate;

    @JdbcTypeCode(SqlTypes.DECIMAL)
    @Column(name = "odometer_at_service", nullable = false)
    private Double odometerAtService;

    @JdbcTypeCode(SqlTypes.DECIMAL)
    @Column(name = "next_service_due_km")
    private Double nextServiceDueKm;

    @Column(name = "next_service_due_date")
    private LocalDate nextServiceDueDate;

    @Column(nullable = false, length = 30)
    private String status = "COMPLETED";

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public MaintenanceRecord() {}

    public MaintenanceRecord(Vehicle vehicle, String serviceType, String description, BigDecimal cost,
                             LocalDate serviceDate, Double odometerAtService, Double nextServiceDueKm,
                             LocalDate nextServiceDueDate, String status) {
        this.vehicle = vehicle;
        this.serviceType = serviceType;
        this.description = description;
        this.cost = cost;
        this.serviceDate = serviceDate;
        this.odometerAtService = odometerAtService;
        this.nextServiceDueKm = nextServiceDueKm;
        this.nextServiceDueDate = nextServiceDueDate;
        this.status = status != null ? status : "COMPLETED";
        this.createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Vehicle getVehicle() { return vehicle; }
    public void setVehicle(Vehicle vehicle) { this.vehicle = vehicle; }

    public String getServiceType() { return serviceType; }
    public void setServiceType(String serviceType) { this.serviceType = serviceType; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public BigDecimal getCost() { return cost; }
    public void setCost(BigDecimal cost) { this.cost = cost; }

    public LocalDate getServiceDate() { return serviceDate; }
    public void setServiceDate(LocalDate serviceDate) { this.serviceDate = serviceDate; }

    public Double getOdometerAtService() { return odometerAtService; }
    public void setOdometerAtService(Double odometerAtService) { this.odometerAtService = odometerAtService; }

    public Double getNextServiceDueKm() { return nextServiceDueKm; }
    public void setNextServiceDueKm(Double nextServiceDueKm) { this.nextServiceDueKm = nextServiceDueKm; }

    public LocalDate getNextServiceDueDate() { return nextServiceDueDate; }
    public void setNextServiceDueDate(LocalDate nextServiceDueDate) { this.nextServiceDueDate = nextServiceDueDate; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
