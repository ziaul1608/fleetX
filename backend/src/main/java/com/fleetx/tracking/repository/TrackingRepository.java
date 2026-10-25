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

    List<TrackingLog> findByVehicleIdAndTenantIdOrderByRecordedAtDesc(Long vehicleId, Long tenantId);

    List<TrackingLog> findByVehicleIdAndTenantIdOrderByRecordedAtDesc(Long vehicleId, Long tenantId, Pageable pageable);

    Optional<TrackingLog> findFirstByVehicleIdAndTenantIdOrderByRecordedAtDesc(Long vehicleId, Long tenantId);

    List<TrackingLog> findByTripIdAndTenantIdOrderByRecordedAtAsc(Long tripId, Long tenantId);

    long countByVehicleIdAndTenantId(Long vehicleId, Long tenantId);

    @Query("""
            SELECT t FROM TrackingLog t
            WHERE t.tenantId = :tenantId
              AND t.id IN (
                SELECT MAX(t2.id) FROM TrackingLog t2 WHERE t2.tenantId = :tenantId GROUP BY t2.vehicleId
            )
            ORDER BY t.recordedAt DESC
            """)
    List<TrackingLog> findLatestPingPerVehicle(@Param("tenantId") Long tenantId);

    @Modifying
    @Query("DELETE FROM TrackingLog t WHERE t.vehicleId = :vehicleId AND t.tenantId = :tenantId AND t.id NOT IN :keepIds")
    void deleteByVehicleIdAndTenantIdAndIdNotIn(@Param("vehicleId") Long vehicleId, @Param("tenantId") Long tenantId, @Param("keepIds") List<Long> keepIds);
}
