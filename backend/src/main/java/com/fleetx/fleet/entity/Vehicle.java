package com.fleetx.fleet.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.Instant;

@Entity
@Table(name = "vehicles")
public class Vehicle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 17)
    private String vin;

    @Column(name = "license_plate", nullable = false, unique = true, length = 20)
    private String licensePlate;

    @Column(nullable = false, length = 60)
    private String make;

    @Column(nullable = false, length = 60)
    private String model;

    @Column(name = "model_year", nullable = false)
    private Integer modelYear;

    @Column(name = "vehicle_type", nullable = false, length = 30)
    private String vehicleType;

    @Column(name = "fuel_type", nullable = false, length = 20)
    private String fuelType;

    @JdbcTypeCode(SqlTypes.DECIMAL)
    @Column(name = "capacity_kg", nullable = false)
    private Double capacityKg;

    @JdbcTypeCode(SqlTypes.DECIMAL)
    @Column(name = "capacity_volume_m3", nullable = false)
    private Double capacityVolumeM3;

    @JdbcTypeCode(SqlTypes.DECIMAL)
    @Column(name = "odometer_km", nullable = false)
    private Double odometerKm = 0.0;

    @Column(nullable = false, length = 30)
    private String status = "AVAILABLE";

    @Column(nullable = false)
    private Boolean active = true;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId = 1L;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    public Vehicle() {}

    public Vehicle(String vin, String licensePlate, String make, String model, Integer modelYear,
                   String vehicleType, String fuelType, Double capacityKg, Double capacityVolumeM3,
                   Double odometerKm, String status) {
        this.vin = vin != null ? vin.trim().toUpperCase() : null;
        this.licensePlate = licensePlate != null ? licensePlate.trim().toUpperCase() : null;
        this.make = make != null ? make.trim() : null;
        this.model = model != null ? model.trim() : null;
        this.modelYear = modelYear;
        this.vehicleType = vehicleType;
        this.fuelType = fuelType;
        this.capacityKg = capacityKg;
        this.capacityVolumeM3 = capacityVolumeM3;
        this.odometerKm = odometerKm != null ? odometerKm : 0.0;
        this.status = status != null ? status : "AVAILABLE";
        this.active = true;
    }

    @PrePersist
    public void onPrePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        updatedAt = Instant.now();
        if (active == null) {
            active = true;
        }
        if (odometerKm == null) {
            odometerKm = 0.0;
        }
    }

    @PreUpdate
    public void onPreUpdate() {
        updatedAt = Instant.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getVin() { return vin; }
    public void setVin(String vin) {
        this.vin = vin != null ? vin.trim().toUpperCase() : null;
    }

    public String getLicensePlate() { return licensePlate; }
    public void setLicensePlate(String licensePlate) {
        this.licensePlate = licensePlate != null ? licensePlate.trim().toUpperCase() : null;
    }

    public String getMake() { return make; }
    public void setMake(String make) {
        this.make = make != null ? make.trim() : null;
    }

    public String getModel() { return model; }
    public void setModel(String model) {
        this.model = model != null ? model.trim() : null;
    }

    public Integer getModelYear() { return modelYear; }
    public void setModelYear(Integer modelYear) { this.modelYear = modelYear; }

    public String getVehicleType() { return vehicleType; }
    public void setVehicleType(String vehicleType) { this.vehicleType = vehicleType; }

    public String getFuelType() { return fuelType; }
    public void setFuelType(String fuelType) { this.fuelType = fuelType; }

    public Double getCapacityKg() { return capacityKg; }
    public void setCapacityKg(Double capacityKg) { this.capacityKg = capacityKg; }

    public Double getCapacityVolumeM3() { return capacityVolumeM3; }
    public void setCapacityVolumeM3(Double capacityVolumeM3) { this.capacityVolumeM3 = capacityVolumeM3; }

    public Double getOdometerKm() { return odometerKm; }
    public void setOdometerKm(Double odometerKm) { this.odometerKm = odometerKm; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public Long getTenantId() { return tenantId; }
    public void setTenantId(Long tenantId) { this.tenantId = tenantId; }
}
