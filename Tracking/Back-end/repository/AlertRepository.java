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

    Optional<Alert> findFirstByAlertTypeAndEntityTypeAndEntityIdAndAcknowledgedFalseOrderByCreatedAtDesc(
            String alertType,
            String entityType,
            Long entityId
    );

    Optional<Alert> findFirstByDedupKeyAndAcknowledgedFalseOrderByCreatedAtDesc(String dedupKey);

    long countByAcknowledgedFalse();

    long countByAcknowledgedFalseAndSeverityIgnoreCase(String severity);

    @Query("""
            SELECT a FROM Alert a
            WHERE (:alertType IS NULL OR :alertType = '' OR UPPER(a.alertType) = UPPER(:alertType))
              AND (:severity IS NULL OR :severity = '' OR UPPER(a.severity) = UPPER(:severity))
              AND (:entityType IS NULL OR :entityType = '' OR UPPER(a.entityType) = UPPER(:entityType))
              AND (:acknowledged IS NULL OR a.acknowledged = :acknowledged)
            ORDER BY a.createdAt DESC
            """)
    Page<Alert> findFiltered(
            @Param("alertType") String alertType,
            @Param("severity") String severity,
            @Param("entityType") String entityType,
            @Param("acknowledged") Boolean acknowledged,
            Pageable pageable
    );

    List<Alert> findTop50ByOrderByCreatedAtDesc();
}
