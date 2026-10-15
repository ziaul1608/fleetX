package com.fleetx.shipments.repository;

import com.fleetx.shipments.entity.ShipmentTimelineEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ShipmentTimelineEventRepository extends JpaRepository<ShipmentTimelineEvent, Long> {
    List<ShipmentTimelineEvent> findByShipmentIdOrderByCreatedAtDesc(Long shipmentId);
    List<ShipmentTimelineEvent> findByShipmentIdOrderByCreatedAtAsc(Long shipmentId);
}
