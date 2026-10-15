package com.fleetx.fleet.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.*;

public class VehicleCreateRequest {

    @NotBlank(message = "VIN is required.")
    @Pattern(regexp = "^[A-HJ-NPR-Z0-9]{17}$", message = "VIN must be exactly 17 uppercase alphanumeric characters (excluding I, O, Q).")
    private String vin;

    @NotBlank(message = "License plate registration is required.")
    @Size(min = 4, max = 20, message = "License plate must be between 4 and 20 characters.")
    private String licensePlate;

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

    @PositiveOrZero(message = "Initial odometer reading cannot be negative.")
    @JsonAlias({"initialOdometerKm"})
    private Double odometerKm = 0.0;

    private String status = "AVAILABLE";

    public VehicleCreateRequest() {}

    public VehicleCreateRequest(String vin, String licensePlate, String make, String model,
                                Integer modelYear, String vehicleType, String fuelType,
                                Double capacityKg, Double capacityVolumeM3, Double odometerKm, String status) {
        this.vin = vin;
        this.licensePlate = licensePlate;
        this.make = make;
        this.model = model;
        this.modelYear = modelYear;
        this.vehicleType = vehicleType;
        this.fuelType = fuelType;
        this.capacityKg = capacityKg;
        this.capacityVolumeM3 = capacityVolumeM3;
        this.odometerKm = odometerKm != null ? odometerKm : 0.0;
        this.status = status != null ? status : "AVAILABLE";
    }

    public String getVin() { return vin; }
    public void setVin(String vin) { this.vin = vin; }

    public String getLicensePlate() { return licensePlate; }
    public void setLicensePlate(String licensePlate) { this.licensePlate = licensePlate; }

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

    public Double getOdometerKm() { return odometerKm; }
    public void setOdometerKm(Double odometerKm) { this.odometerKm = odometerKm; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
