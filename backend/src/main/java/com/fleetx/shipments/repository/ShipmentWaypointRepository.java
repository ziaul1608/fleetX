package com.fleetx.shipments.repository;

import com.fleetx.shipments.entity.ShipmentWaypoint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ShipmentWaypointRepository extends JpaRepository<ShipmentWaypoint, Long> {
    List<ShipmentWaypoint> findByShipmentIdOrderBySequenceOrderAsc(Long shipmentId);
    void deleteByShipmentId(Long shipmentId);
}
