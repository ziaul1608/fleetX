package com.fleetx.fleet.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.time.LocalDate;

public class MaintenanceRecordCreateRequest {

    @NotBlank(message = "Service type is required.")
    private String serviceType;

    @NotBlank(message = "Description of service performed is required.")
    private String description;

    @NotNull(message = "Service cost is required.")
    @PositiveOrZero(message = "Cost cannot be negative.")
    private BigDecimal cost;

    @NotNull(message = "Service date is required.")
    private LocalDate serviceDate;

    @NotNull(message = "Odometer reading at service is required.")
    @PositiveOrZero(message = "Odometer reading cannot be negative.")
    private Double odometerAtService;

    @PositiveOrZero(message = "Next service due km cannot be negative.")
    private Double nextServiceDueKm;

    private LocalDate nextServiceDueDate;

    private String status = "COMPLETED";

    public MaintenanceRecordCreateRequest() {}

    public MaintenanceRecordCreateRequest(String serviceType, String description, BigDecimal cost,
                                          LocalDate serviceDate, Double odometerAtService,
                                          Double nextServiceDueKm, LocalDate nextServiceDueDate, String status) {
        this.serviceType = serviceType;
        this.description = description;
        this.cost = cost;
        this.serviceDate = serviceDate;
        this.odometerAtService = odometerAtService;
        this.nextServiceDueKm = nextServiceDueKm;
        this.nextServiceDueDate = nextServiceDueDate;
        this.status = status != null ? status : "COMPLETED";
    }

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
}
