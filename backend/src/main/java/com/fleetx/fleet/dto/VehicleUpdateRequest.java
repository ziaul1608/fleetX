package com.fleetx.fleet.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.*;

public class VehicleUpdateRequest {

    @NotBlank(message = "Vehicle make manufacturer is required.")
    @Size(max = 60, message = "Make must not exceed 60 characters.")
    private String make;

    @NotBlank(message = "Vehicle model is required.")
    @Size(max = 60, message = "Model must not exceed 60 characters.")
    private String model;

    @NotNull(message = "Manufacturing model year is required.")
    @Min(value = 1980, message = "Model year must be 1980 or later.")
    @Max(value = 2030, message = "Model year cannot be in the distant future.")
    @JsonAlias({"year"})
    private Integer modelYear;

    @NotBlank(message = "Vehicle type is required.")
    @JsonAlias({"type"})
    private String vehicleType;

    @NotBlank(message = "Fuel type is required.")
    private String fuelType;

    @NotNull(message = "Payload capacity in kg is required.")
    @Positive(message = "Capacity kg must be positive.")
    private Double capacityKg;

    @NotNull(message = "Capacity volume in cubic meters is required.")
    @Positive(message = "Capacity volume must be positive.")
    private Double capacityVolumeM3;

    @NotBlank(message = "Operational status is required.")
    private String status;

    @NotNull(message = "Active state flag is required.")
    private Boolean active;

    public VehicleUpdateRequest() {}

    public VehicleUpdateRequest(String make, String model, Integer modelYear, String vehicleType,
                                String fuelType, Double capacityKg, Double capacityVolumeM3,
                                String status, Boolean active) {
        this.make = make;
        this.model = model;
        this.modelYear = modelYear;
        this.vehicleType = vehicleType;
        this.fuelType = fuelType;
        this.capacityKg = capacityKg;
        this.capacityVolumeM3 = capacityVolumeM3;
        this.status = status;
        this.active = active;
    }

    public String getMake() { return make; }
    public void setMake(String make) { this.make = make; }

    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }

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

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
}
