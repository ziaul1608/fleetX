package com.fleetx.shipments.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record RoutePlanResponseDto(
        Long shipmentId,
        String trackingNumber,
        BigDecimal estimatedDistanceKm,
        Integer estimatedDurationMinutes,
        Instant estimatedArrivalTime,
        String distanceLabel,
        String etaLabel,
        Double averageSpeedAssumedKmh,
        Integer stopAllowanceMinutesPerStop,
        List<WaypointResponseDto> waypoints
) {
    @JsonProperty("totalEstimatedDistanceKm")
    public BigDecimal totalEstimatedDistanceKm() {
        return estimatedDistanceKm;
    }

    @JsonProperty("assumedAverageSpeedKmh")
    public Double assumedAverageSpeedKmh() {
        return averageSpeedAssumedKmh;
    }

    @JsonProperty("stopAllowanceMinutesPerIntermediateStop")
    public Integer stopAllowanceMinutesPerIntermediateStop() {
        return stopAllowanceMinutesPerStop;
    }

    @JsonProperty("distanceEstimationMethod")
    public String distanceEstimationMethod() {
        return distanceLabel;
    }

    @JsonProperty("etaEstimationMethod")
    public String etaEstimationMethod() {
        return etaLabel;
    }
}
