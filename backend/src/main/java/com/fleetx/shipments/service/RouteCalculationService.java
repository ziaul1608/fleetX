package com.fleetx.shipments.service;

import com.fleetx.shared.exception.DomainException;
import com.fleetx.shipments.dto.WaypointItemRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;

@Service
public class RouteCalculationService {

    public static final double EARTH_RADIUS_KM = 6371.0;
    public static final String DISTANCE_ESTIMATE_LABEL = "Estimated Straight-Line Distance (Haversine Formula — Not Exact Road Distance)";
    public static final String ETA_ESTIMATE_LABEL = "Estimated Arrival Time (Assumed Constant Speed & Stop Allowance — Not Live Traffic Prediction)";

    private final double averageSpeedKmh;
    private final int stopAllowanceMinutes;

    public RouteCalculationService(
            @Value("${fleetx.routing.average-speed-kmh:50.0}") double averageSpeedKmh,
            @Value("${fleetx.routing.stop-allowance-minutes:15}") int stopAllowanceMinutes
    ) {
        this.averageSpeedKmh = averageSpeedKmh > 0 ? averageSpeedKmh : 50.0;
        this.stopAllowanceMinutes = Math.max(0, stopAllowanceMinutes);
    }

    public double getAverageSpeedKmh() {
        return averageSpeedKmh;
    }

    public int getStopAllowanceMinutes() {
        return stopAllowanceMinutes;
    }

    /**
     * Validates a waypoint list and sorts by sequenceOrder.
     */
    public List<WaypointItemRequest> validateAndSortWaypoints(List<WaypointItemRequest> waypoints) {
        if (waypoints == null || waypoints.size() < 2) {
            throw new DomainException("At least 2 waypoints (Origin and Destination) are required for route planning.",
                    HttpStatus.BAD_REQUEST, "INVALID_WAYPOINTS");
        }

        for (WaypointItemRequest wp : waypoints) {
            if (wp.latitude() == null || wp.latitude() < -90.0 || wp.latitude() > 90.0) {
                throw new DomainException("Invalid latitude '" + wp.latitude() + "' for waypoint '" + wp.waypointName() + "'. Must be between -90.0 and 90.0.",
                        HttpStatus.BAD_REQUEST, "INVALID_COORDINATES");
            }
            if (wp.longitude() == null || wp.longitude() < -180.0 || wp.longitude() > 180.0) {
                throw new DomainException("Invalid longitude '" + wp.longitude() + "' for waypoint '" + wp.waypointName() + "'. Must be between -180.0 and 180.0.",
                        HttpStatus.BAD_REQUEST, "INVALID_COORDINATES");
            }
        }

        return waypoints.stream()
                .sorted(Comparator.comparingInt(WaypointItemRequest::sequenceOrder))
                .toList();
    }

    /**
     * Computes Haversine straight-line distance in kilometers between two geographic coordinates.
     */
    public double calculateHaversineKm(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double rLat1 = Math.toRadians(lat1);
        double rLat2 = Math.toRadians(lat2);

        double a = Math.sin(dLat / 2.0) * Math.sin(dLat / 2.0)
                + Math.cos(rLat1) * Math.cos(rLat2) * Math.sin(dLon / 2.0) * Math.sin(dLon / 2.0);
        double c = 2.0 * Math.atan2(Math.sqrt(a), Math.sqrt(Math.max(0.0, 1.0 - a)));

        return EARTH_RADIUS_KM * c;
    }

    /**
     * Calculates total estimated route distance across ordered waypoints.
     */
    public BigDecimal calculateTotalRouteDistanceKm(List<WaypointItemRequest> sortedWaypoints) {
        if (sortedWaypoints == null || sortedWaypoints.size() < 2) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        double totalKm = 0.0;
        for (int i = 1; i < sortedWaypoints.size(); i++) {
            WaypointItemRequest prev = sortedWaypoints.get(i - 1);
            WaypointItemRequest curr = sortedWaypoints.get(i);
            totalKm += calculateHaversineKm(prev.latitude(), prev.longitude(), curr.latitude(), curr.longitude());
        }

        return BigDecimal.valueOf(totalKm).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Estimates journey duration in minutes based on distance, configurable average speed, and intermediate stop allowances.
     */
    public int estimateDurationMinutes(BigDecimal distanceKm, int totalWaypointsCount) {
        if (distanceKm == null || distanceKm.compareTo(BigDecimal.ZERO) <= 0) {
            return 0;
        }
        double travelMinutes = (distanceKm.doubleValue() / averageSpeedKmh) * 60.0;
        int intermediateStops = Math.max(0, totalWaypointsCount - 2);
        int allowance = intermediateStops * stopAllowanceMinutes;

        return (int) Math.round(travelMinutes) + allowance;
    }
}
