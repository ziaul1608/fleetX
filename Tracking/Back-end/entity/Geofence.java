package com.fleetx.tracking.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.Instant;

@Entity
@Table(name = "geofences")
public class Geofence {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @JdbcTypeCode(SqlTypes.DECIMAL)
    @Column(name = "center_latitude", nullable = false)
    private Double centerLatitude;

    @JdbcTypeCode(SqlTypes.DECIMAL)
    @Column(name = "center_longitude", nullable = false)
    private Double centerLongitude;

    @JdbcTypeCode(SqlTypes.DECIMAL)
    @Column(name = "radius_meters", nullable = false)
    private Double radiusMeters;

    @Column(name = "fence_type", nullable = false, length = 30)
    private String fenceType = "DEPOT";

    @Column(name = "shape_type", nullable = false, length = 20)
    private String shapeType = "CIRCLE";

    @Column(name = "polygon_coordinates_json", columnDefinition = "TEXT")
    private String polygonCoordinatesJson;

    @JdbcTypeCode(SqlTypes.DECIMAL)
    @Column(name = "speed_limit_kmh")
    private Double speedLimitKmh;

    @Column(nullable = false)
    private Boolean active = true;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    public Geofence() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Double getCenterLatitude() { return centerLatitude; }
    public void setCenterLatitude(Double centerLatitude) { this.centerLatitude = centerLatitude; }

    public Double getCenterLongitude() { return centerLongitude; }
    public void setCenterLongitude(Double centerLongitude) { this.centerLongitude = centerLongitude; }

    public Double getRadiusMeters() { return radiusMeters; }
    public void setRadiusMeters(Double radiusMeters) { this.radiusMeters = radiusMeters; }

    public String getFenceType() { return fenceType; }
    public void setFenceType(String fenceType) { this.fenceType = fenceType; }

    public String getShapeType() { return shapeType; }
    public void setShapeType(String shapeType) { this.shapeType = shapeType; }

    public String getPolygonCoordinatesJson() { return polygonCoordinatesJson; }
    public void setPolygonCoordinatesJson(String polygonCoordinatesJson) { this.polygonCoordinatesJson = polygonCoordinatesJson; }

    public Double getSpeedLimitKmh() { return speedLimitKmh; }
    public void setSpeedLimitKmh(Double speedLimitKmh) { this.speedLimitKmh = speedLimitKmh; }

    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
