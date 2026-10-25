package com.fleetx.tracking.repository;

import com.fleetx.tracking.entity.Alert;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AlertRepository extends JpaRepository<Alert, Long> {

    Optional<Alert> findByIdAndTenantId(Long id, Long tenantId);

    Optional<Alert> findFirstByTenantIdAndAlertTypeAndEntityTypeAndEntityIdAndAcknowledgedFalseOrderByCreatedAtDesc(
            Long tenantId,
            String alertType,
            String entityType,
            Long entityId
    );

    Optional<Alert> findFirstByTenantIdAndDedupKeyAndAcknowledgedFalseOrderByCreatedAtDesc(Long tenantId, String dedupKey);

    long countByTenantIdAndAcknowledgedFalse(Long tenantId);

    long countByTenantIdAndAcknowledgedFalseAndSeverityIgnoreCase(Long tenantId, String severity);

    long countByTenantId(Long tenantId);

    @Query("""
            SELECT a FROM Alert a
            WHERE a.tenantId = :tenantId
              AND (:alertType IS NULL OR :alertType = '' OR UPPER(a.alertType) = UPPER(:alertType))
              AND (:severity IS NULL OR :severity = '' OR UPPER(a.severity) = UPPER(:severity))
              AND (:entityType IS NULL OR :entityType = '' OR UPPER(a.entityType) = UPPER(:entityType))
              AND (:acknowledged IS NULL OR a.acknowledged = :acknowledged)
            ORDER BY a.createdAt DESC
            """)
    Page<Alert> findFiltered(
            @Param("tenantId") Long tenantId,
            @Param("alertType") String alertType,
            @Param("severity") String severity,
            @Param("entityType") String entityType,
            @Param("acknowledged") Boolean acknowledged,
            Pageable pageable
    );

    List<Alert> findTop50ByTenantIdOrderByCreatedAtDesc(Long tenantId);
}
