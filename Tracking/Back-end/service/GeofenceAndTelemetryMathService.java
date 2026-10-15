package com.fleetx.tracking.service;

import com.fleetx.shared.exception.DomainException;
import com.fleetx.tracking.dto.SimulatorWaypointDto;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Pure geographical and kinematic computation service for simulated GPS interpolation,
 * heading/speed calculation, and circular/polygonal geofence boundary evaluation.
 */
@Service
public class GeofenceAndTelemetryMathService {

    public static final double EARTH_RADIUS_KM = 6371.0;
    public static final double EARTH_RADIUS_METERS = 6371000.0;
    public static final double MAX_ALLOWED_SPEED_KMH = 300.0;

    public void validateCoordinates(Double latitude, Double longitude) {
        if (latitude == null || longitude == null
                || Double.isNaN(latitude) || Double.isInfinite(latitude)
                || Double.isNaN(longitude) || Double.isInfinite(longitude)) {
            throw new DomainException("Latitude and longitude must be valid finite numbers.", HttpStatus.BAD_REQUEST, "INVALID_COORDINATES");
        }
        if (latitude < -90.0 || latitude > 90.0) {
            throw new DomainException(
                    "Latitude " + latitude + " is out of bounds. Must be between -90.0 and 90.0 degrees.",
                    HttpStatus.BAD_REQUEST,
                    "INVALID_LATITUDE"
            );
        }
        if (longitude < -180.0 || longitude > 180.0) {
            throw new DomainException(
                    "Longitude " + longitude + " is out of bounds. Must be between -180.0 and 180.0 degrees.",
                    HttpStatus.BAD_REQUEST,
                    "INVALID_LONGITUDE"
            );
        }
    }

    public void validateTelemetryInputs(Double latitude, Double longitude, Double speedKmh, Instant timestamp) {
        validateCoordinates(latitude, longitude);
        if (speedKmh == null || Double.isNaN(speedKmh) || Double.isInfinite(speedKmh)
                || speedKmh < 0.0 || speedKmh > MAX_ALLOWED_SPEED_KMH) {
            throw new DomainException(
                    "Speed (km/h) must be between 0.0 and " + MAX_ALLOWED_SPEED_KMH + ".",
                    HttpStatus.BAD_REQUEST,
                    "INVALID_SPEED"
            );
        }
        if (timestamp == null) {
            throw new DomainException("Telemetry timestamp is required.", HttpStatus.BAD_REQUEST, "INVALID_TIMESTAMP");
        }
        if (timestamp.isAfter(Instant.now().plus(1, ChronoUnit.DAYS))) {
            throw new DomainException("Telemetry timestamp cannot be in the far future.", HttpStatus.BAD_REQUEST, "INVALID_TIMESTAMP");
        }
    }

    public double haversineDistanceKm(double lat1, double lon1, double lat2, double lon2) {
        validateCoordinates(lat1, lon1);
        validateCoordinates(lat2, lon2);

        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(dLat / 2.0) * Math.sin(dLat / 2.0)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2.0) * Math.sin(dLon / 2.0);

        double c = 2.0 * Math.atan2(Math.sqrt(Math.min(1.0, a)), Math.sqrt(Math.max(0.0, 1.0 - a)));
        return EARTH_RADIUS_KM * c;
    }

    public double haversineDistanceMeters(double lat1, double lon1, double lat2, double lon2) {
        return haversineDistanceKm(lat1, lon1, lat2, lon2) * 1000.0;
    }

    /**
     * Calculates compass bearing in degrees [0, 360).
     * Safely returns normalized fallbackHeading if the vehicle is stationary (identical coordinates).
     */
    public double calculateBearingDegrees(double lat1, double lon1, double lat2, double lon2, double fallbackHeading) {
        validateCoordinates(lat1, lon1);
        validateCoordinates(lat2, lon2);

        if (Math.abs(lat2 - lat1) < 1e-8 && Math.abs(lon2 - lon1) < 1e-8) {
            double safeFallback = (Double.isNaN(fallbackHeading) || Double.isInfinite(fallbackHeading)) ? 0.0 : fallbackHeading;
            return ((safeFallback % 360.0) + 360.0) % 360.0;
        }

        double phi1 = Math.toRadians(lat1);
        double phi2 = Math.toRadians(lat2);
        double deltaLambda = Math.toRadians(lon2 - lon1);

        double y = Math.sin(deltaLambda) * Math.cos(phi2);
        double x = Math.cos(phi1) * Math.sin(phi2)
                - Math.sin(phi1) * Math.cos(phi2) * Math.cos(deltaLambda);

        double theta = Math.toDegrees(Math.atan2(y, x));
        double bearing = (theta + 360.0) % 360.0;
        return Math.round(bearing * 100.0) / 100.0;
    }

    /**
     * Safely calculates speed in km/h from distance (km) and elapsed time (seconds).
     * Handles stationary vehicles and very short or non-positive time intervals without NaN/Infinity.
     */
    public double calculateSafeSpeedKmh(double distanceKm, double elapsedSeconds) {
        if (Double.isNaN(distanceKm) || Double.isInfinite(distanceKm) || distanceKm <= 1e-6) {
            return 0.0;
        }
        if (Double.isNaN(elapsedSeconds) || Double.isInfinite(elapsedSeconds) || elapsedSeconds <= 0.05) {
            return 0.0;
        }
        double rawSpeed = (distanceKm / elapsedSeconds) * 3600.0;
        double clamped = Math.min(MAX_ALLOWED_SPEED_KMH, Math.max(0.0, rawSpeed));
        return Math.round(clamped * 100.0) / 100.0;
    }

    /**
     * Smoothly interpolates between start and end waypoints for fraction in [0.0, 1.0].
     */
    public SimulatorWaypointDto interpolatePoint(
            SimulatorWaypointDto start,
            SimulatorWaypointDto end,
            double fraction
    ) {
        validateCoordinates(start.latitude(), start.longitude());
        validateCoordinates(end.latitude(), end.longitude());

        double clamped = Math.max(0.0, Math.min(1.0, fraction));
        double lat = start.latitude() + clamped * (end.latitude() - start.latitude());
        double lon = start.longitude() + clamped * (end.longitude() - start.longitude());

        double roundedLat = Math.round(lat * 1_000_000.0) / 1_000_000.0;
        double roundedLon = Math.round(lon * 1_000_000.0) / 1_000_000.0;
        String label = clamped >= 1.0 ? end.label() : start.label();
        return new SimulatorWaypointDto(roundedLat, roundedLon, label);
    }

    public boolean isInsideCircle(double lat, double lon, double centerLat, double centerLon, double radiusMeters) {
        if (radiusMeters <= 0) {
            return false;
        }
        double distanceMeters = haversineDistanceMeters(lat, lon, centerLat, centerLon);
        return distanceMeters <= radiusMeters;
    }

    /**
     * Ray-casting Point-in-Polygon boundary check for polygonal geofences.
     * Each vertex is [latitude, longitude].
     */
    public boolean isInsidePolygon(double lat, double lon, List<List<Double>> polygonVertices) {
        if (polygonVertices == null || polygonVertices.size() < 3) {
            return false;
        }

        boolean inside = false;
        int n = polygonVertices.size();
        for (int i = 0, j = n - 1; i < n; j = i++) {
            List<Double> vi = polygonVertices.get(i);
            List<Double> vj = polygonVertices.get(j);
            if (vi == null || vj == null || vi.size() < 2 || vj.size() < 2) {
                continue;
            }
            double yi = vi.get(0); // lat
            double xi = vi.get(1); // lon
            double yj = vj.get(0); // lat
            double xj = vj.get(1); // lon

            // Vertex coincidence check
            if (Math.abs(lat - yi) < 1e-7 && Math.abs(lon - xi) < 1e-7) {
                return true;
            }

            boolean intersect = ((yi > lat) != (yj > lat))
                    && (lon < (xj - xi) * (lat - yi) / ((yj - yi) + 1e-15) + xi);
            if (intersect) {
                inside = !inside;
            }
        }
        return inside;
    }
}
