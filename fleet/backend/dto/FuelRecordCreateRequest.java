package com.fleetx.fleet.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.time.Instant;

public class FuelRecordCreateRequest {

    @NotNull(message = "Liters consumed is required.")
    @Positive(message = "Liters must be greater than zero.")
    private Double liters;

    @Positive(message = "Price per liter must be positive.")
    private BigDecimal pricePerLiter;

    @Positive(message = "Total cost must be positive.")
    private BigDecimal totalCost;

    @NotNull(message = "Odometer reading at fueling is required.")
    @PositiveOrZero(message = "Odometer reading cannot be negative.")
    private Double odometerAtFueling;

    private String fuelStation;

    private Instant fuelDate;

    public FuelRecordCreateRequest() {}

    public FuelRecordCreateRequest(Double liters, BigDecimal pricePerLiter, BigDecimal totalCost,
                                   Double odometerAtFueling, String fuelStation, Instant fuelDate) {
        this.liters = liters;
        this.pricePerLiter = pricePerLiter;
        this.totalCost = totalCost;
        this.odometerAtFueling = odometerAtFueling;
        this.fuelStation = fuelStation;
        this.fuelDate = fuelDate;
    }

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
}
