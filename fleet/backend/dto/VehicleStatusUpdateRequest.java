package com.fleetx.fleet.dto;

import jakarta.validation.constraints.NotBlank;

public class VehicleStatusUpdateRequest {

    @NotBlank(message = "Vehicle status is required.")
    private String status;

    private String reason;

    public VehicleStatusUpdateRequest() {}

    public VehicleStatusUpdateRequest(String status, String reason) {
        this.status = status;
        this.reason = reason;
    }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
