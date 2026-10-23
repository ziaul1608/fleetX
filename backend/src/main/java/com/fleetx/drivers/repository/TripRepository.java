package com.fleetx.drivers.repository;

import com.fleetx.drivers.entity.Trip;
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
public interface TripRepository extends JpaRepository<Trip, Long> {

    Optional<Trip> findByTripCode(String tripCode);

    boolean existsByTripCode(String tripCode);

    List<Trip> findByDriverId(Long driverId);

    List<Trip> findByVehicleId(Long vehicleId);

    List<Trip> findByStatus(String status);

    @Query("SELECT t FROM Trip t WHERE " +
            "(:search IS NULL OR :search = '' OR " +
            " LOWER(t.tripCode) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            " LOWER(t.originAddress) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            " LOWER(t.destinationAddress) LIKE LOWER(CONCAT('%', :search, '%'))) " +
            "AND (:status IS NULL OR :status = '' OR t.status = :status) " +
            "AND (:driverId IS NULL OR t.driverId = :driverId) " +
            "AND (:vehicleId IS NULL OR t.vehicleId = :vehicleId)")
    Page<Trip> findWithFilters(@Param("search") String search,
                               @Param("status") String status,
                               @Param("driverId") Long driverId,
                               @Param("vehicleId") Long vehicleId,
                               Pageable pageable);

    long countByStatus(String status);

    long countByDriverId(Long driverId);

    long countByDriverIdAndStatus(Long driverId, String status);

    @Query("SELECT COUNT(t) FROM Trip t WHERE t.scheduledStartTime >= :startOfDay AND t.scheduledStartTime < :endOfDay")
    long countTripsDispatchedOnDate(@Param("startOfDay") Instant startOfDay, @Param("endOfDay") Instant endOfDay);

    @Query("SELECT COUNT(t) > 0 FROM Trip t WHERE t.vehicleId = :vehicleId AND t.status IN :activeStatuses")
    boolean existsByVehicleIdAndStatusIn(@Param("vehicleId") Long vehicleId, @Param("activeStatuses") java.util.Collection<String> activeStatuses);
}
