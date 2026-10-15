package com.fleetx.tracking.repository;

import com.fleetx.tracking.entity.TrackingLog;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TrackingRepository extends JpaRepository<TrackingLog, Long> {

    List<TrackingLog> findByVehicleIdOrderByRecordedAtDesc(Long vehicleId);

    List<TrackingLog> findByVehicleIdOrderByRecordedAtDesc(Long vehicleId, Pageable pageable);

    Optional<TrackingLog> findFirstByVehicleIdOrderByRecordedAtDesc(Long vehicleId);

    List<TrackingLog> findByTripIdOrderByRecordedAtAsc(Long tripId);

    long countByVehicleId(Long vehicleId);

    @Query("""
            SELECT t FROM TrackingLog t
            WHERE t.id IN (
                SELECT MAX(t2.id) FROM TrackingLog t2 GROUP BY t2.vehicleId
            )
            ORDER BY t.recordedAt DESC
            """)
    List<TrackingLog> findLatestPingPerVehicle();

    @Modifying
    @Query("DELETE FROM TrackingLog t WHERE t.vehicleId = :vehicleId AND t.id NOT IN :keepIds")
    void deleteByVehicleIdAndIdNotIn(@Param("vehicleId") Long vehicleId, @Param("keepIds") List<Long> keepIds);
}
