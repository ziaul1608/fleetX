package com.fleetx.auth.repository;

import com.fleetx.auth.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    Page<AuditLog> findAllByOrderByCreatedAtDesc(Pageable pageable);
    Page<AuditLog> findByActionOrderByCreatedAtDesc(String action, Pageable pageable);
    Page<AuditLog> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    Page<AuditLog> findByTenantIdOrderByCreatedAtDesc(Long tenantId, Pageable pageable);
    Page<AuditLog> findByTenantIdAndActionOrderByCreatedAtDesc(Long tenantId, String action, Pageable pageable);
    Page<AuditLog> findByTenantIdAndUserIdOrderByCreatedAtDesc(Long tenantId, Long userId, Pageable pageable);
}
