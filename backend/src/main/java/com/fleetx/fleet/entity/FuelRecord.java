package com.fleetx.fleet.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "fuel_records")
public class FuelRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId = 1L;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id", nullable = false)
    private Vehicle vehicle;

    @JdbcTypeCode(SqlTypes.DECIMAL)
    @Column(nullable = false)
    private Double liters;

    @Column(name = "price_per_liter", precision = 10, scale = 2)
    private BigDecimal pricePerLiter;

    @Column(name = "total_cost", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalCost;

    @JdbcTypeCode(SqlTypes.DECIMAL)
    @Column(name = "odometer_at_fueling", nullable = false)
    private Double odometerAtFueling;

    @Column(name = "fuel_station", length = 120)
    private String fuelStation;

    @Column(name = "fuel_date", nullable = false)
    private Instant fuelDate;

    @JdbcTypeCode(SqlTypes.DECIMAL)
    @Column(name = "efficiency_km_per_liter")
    private Double efficiencyKmPerLiter;

    public FuelRecord() {}

    public FuelRecord(Vehicle vehicle, Double liters, BigDecimal pricePerLiter, BigDecimal totalCost,
                      Double odometerAtFueling, String fuelStation, Instant fuelDate) {
        this.vehicle = vehicle;
        this.liters = liters;
        this.pricePerLiter = pricePerLiter;
        this.totalCost = totalCost;
        this.odometerAtFueling = odometerAtFueling;
        this.fuelStation = fuelStation;
        this.fuelDate = fuelDate != null ? fuelDate : Instant.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Vehicle getVehicle() { return vehicle; }
    public void setVehicle(Vehicle vehicle) { this.vehicle = vehicle; }

    public Double getLiters() { return liters; }
    public void setLiters(Double liters) { this.liters = liters; }

    public BigDecimal getPricePerLiter() { return pricePerLiter; }
    public void setPricePerLiter(BigDecimal pricePerLiter) { this.pricePerLiter = pricePerLiter; }

    public BigDecimal getTotalCost() { return totalCost; }
    public void setTotalCost(BigDecimal totalCost) { this.totalCost = totalCost; }

    public Double getOdometerAtFueling() { return odometerAtFueling; }
    public void setOdometerAtFueling(Double odometerAtFueling) { this.odometerAtFueling = odometerAtFueling; }

    public String getFuelStation() { return fuelStation; }
    public void setFuelStation(String fuelStation) { this.fuelStation = fuelStation; }

    public Instant getFuelDate() { return fuelDate; }
    public void setFuelDate(Instant fuelDate) { this.fuelDate = fuelDate; }

    public Double getEfficiencyKmPerLiter() { return efficiencyKmPerLiter; }
    public void setEfficiencyKmPerLiter(Double efficiencyKmPerLiter) { this.efficiencyKmPerLiter = efficiencyKmPerLiter; }

    public Long getTenantId() { return tenantId; }
    public void setTenantId(Long tenantId) { this.tenantId = tenantId; }
}
