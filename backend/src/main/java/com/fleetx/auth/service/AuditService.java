package com.fleetx.auth.service;

import com.fleetx.auth.dto.AuditLogResponseDto;
import com.fleetx.auth.entity.AuditLog;
import com.fleetx.auth.repository.AuditLogRepository;
import com.fleetx.shared.dto.PageResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditService.class);
    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordEvent(Long userId, String action, String entityType, Long entityId, String ipAddress, String detailsJson) {
        recordEvent(null, userId, action, entityType, entityId, ipAddress, detailsJson);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordEvent(Long explicitTenantId, Long userId, String action, String entityType, Long entityId, String ipAddress, String detailsJson) {
        try {
            Long tenantId = explicitTenantId != null
                    ? explicitTenantId
                    : com.fleetx.shared.context.TenantSecurityContext.getOptionalTenantId().orElse(null);

            if (tenantId != null) {
                com.fleetx.shared.context.TenantSecurityContext.runWithTenant(tenantId, () -> {
                    AuditLog auditLog = new AuditLog(tenantId, userId, action, entityType, entityId, ipAddress, detailsJson);
                    auditLogRepository.save(auditLog);
                });
                log.info("[AUDIT] Tenant: #{} | Action: {} | User: #{} | Entity: {} #{} | IP: {}",
                        tenantId, action, userId, entityType, entityId, ipAddress);
            } else {
                log.info("[SYSTEM AUDIT] Action: {} | User: #{} | Entity: {} #{} | IP: {} | Details: {}",
                        action, userId, entityType, entityId, ipAddress, detailsJson);
            }
        } catch (Exception ex) {
            log.error("[AUDIT ERROR] Failed to persist audit log for action: " + action, ex);
        }
    }

    @Transactional(readOnly = true)
    public PageResponse<AuditLogResponseDto> getAuditLogs(String action, Long userId, Pageable pageable) {
        Long tenantId = com.fleetx.shared.context.TenantSecurityContext.getCurrentTenantId();
        Page<AuditLog> page;
        if (action != null && !action.isBlank()) {
            page = auditLogRepository.findByTenantIdAndActionOrderByCreatedAtDesc(tenantId, action.trim().toUpperCase(), pageable);
        } else if (userId != null) {
            page = auditLogRepository.findByTenantIdAndUserIdOrderByCreatedAtDesc(tenantId, userId, pageable);
        } else {
            page = auditLogRepository.findByTenantIdOrderByCreatedAtDesc(tenantId, pageable);
        }

        List<AuditLogResponseDto> content = page.getContent().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());

        return new PageResponse<>(
                content,
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isLast()
        );
    }

    private AuditLogResponseDto mapToDto(AuditLog log) {
        return new AuditLogResponseDto(
                log.getId(),
                log.getUserId(),
                log.getAction(),
                log.getEntityType(),
                log.getEntityId(),
                log.getIpAddress(),
                log.getDetailsJson(),
                log.getCreatedAt()
        );
    }
}
