package com.fleetx.shipments.repository;

import com.fleetx.shipments.entity.Shipment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface ShipmentRepository extends JpaRepository<Shipment, Long> {

    Optional<Shipment> findByTrackingNumber(String trackingNumber);

    boolean existsByTrackingNumber(String trackingNumber);

    List<Shipment> findByTripId(Long tripId);

    List<Shipment> findByStatus(String status);

    @Query("SELECT s FROM Shipment s WHERE " +
            "(:search IS NULL OR :search = '' OR " +
            " LOWER(s.trackingNumber) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            " LOWER(s.senderName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            " LOWER(s.receiverName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            " LOWER(s.senderAddress) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            " LOWER(s.receiverAddress) LIKE LOWER(CONCAT('%', :search, '%'))) " +
            "AND (:status IS NULL OR :status = '' OR s.status = :status) " +
            "AND (:tripId IS NULL OR s.tripId = :tripId)")
    Page<Shipment> findWithFilters(@Param("search") String search,
                                   @Param("status") String status,
                                   @Param("tripId") Long tripId,
                                   Pageable pageable);

    @Query("SELECT s FROM Shipment s WHERE s.promisedDeliveryTime IS NOT NULL " +
            "AND s.promisedDeliveryTime < :now " +
            "AND s.status NOT IN ('DELIVERED', 'CANCELLED', 'DELAYED')")
    List<Shipment> findOverdueShipments(@Param("now") Instant now);
}
