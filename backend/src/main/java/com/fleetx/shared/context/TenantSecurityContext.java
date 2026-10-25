package com.fleetx.shared.context;

import com.fleetx.auth.security.FleetxUserPrincipal;
import com.fleetx.shared.exception.DomainException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;
import java.util.function.Supplier;

/**
 * Manages tenant execution context fail-closed.
 * Guarantees that tenant-scoped operations cannot silently fall back to a default tenant.
 */
public final class TenantSecurityContext {

    private static final ThreadLocal<Long> TENANT_OVERRIDE = new ThreadLocal<>();

    private TenantSecurityContext() {}

    public static void setTenantId(Long tenantId) {
        if (tenantId == null) {
            TENANT_OVERRIDE.remove();
        } else {
            TENANT_OVERRIDE.set(tenantId);
        }
    }

    public static void clear() {
        TENANT_OVERRIDE.remove();
    }

    public static Long getExplicitTenantOverride() {
        return TENANT_OVERRIDE.get();
    }

    /**
     * Executes the supplied action within an explicit, trusted tenant context,
     * restoring any previous context and guaranteeing cleanup in a finally block.
     */
    public static <T> T executeWithTenant(Long tenantId, Supplier<T> supplier) {
        Long previous = TENANT_OVERRIDE.get();
        setTenantId(tenantId);
        try {
            return supplier.get();
        } finally {
            setTenantId(previous);
        }
    }

    /**
     * Runs the supplied runnable within an explicit, trusted tenant context,
     * restoring any previous context and guaranteeing cleanup in a finally block.
     */
    public static void runWithTenant(Long tenantId, Runnable runnable) {
        Long previous = TENANT_OVERRIDE.get();
        setTenantId(tenantId);
        try {
            runnable.run();
        } finally {
            setTenantId(previous);
        }
    }

    public static Long getCurrentTenantId() {
        Long override = TENANT_OVERRIDE.get();
        if (override != null) {
            return override;
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new DomainException("Authentication required to access tenant-scoped resources.",
                    HttpStatus.UNAUTHORIZED, "UNAUTHORIZED");
        }

        Object principal = auth.getPrincipal();
        if (principal instanceof FleetxUserPrincipal userPrincipal) {
            Long tid = userPrincipal.getTenantId();
            if (tid == null) {
                throw new DomainException("Active tenant context is required for this operation.",
                        HttpStatus.BAD_REQUEST, "TENANT_CONTEXT_REQUIRED");
            }
            return tid;
        }

        // Fail closed: Do NOT fall back to any default tenant.
        throw new DomainException("Active tenant context is required for this operation.",
                HttpStatus.BAD_REQUEST, "TENANT_CONTEXT_REQUIRED");
    }

    public static Long requireTenantId() {
        return getCurrentTenantId();
    }

    public static Optional<Long> getOptionalTenantId() {
        Long override = TENANT_OVERRIDE.get();
        if (override != null) {
            return Optional.of(override);
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return Optional.empty();
        }

        Object principal = auth.getPrincipal();
        if (principal instanceof FleetxUserPrincipal userPrincipal) {
            return Optional.ofNullable(userPrincipal.getTenantId());
        }

        // Fail closed: No default tenant fallback.
        return Optional.empty();
    }

    /**
     * Validates and assigns tenant ID for entity persistence (@PrePersist).
     * Rejects missing tenant context with 400 (TENANT_CONTEXT_REQUIRED).
     * Rejects mismatched tenant context with 403 (TENANT_ACCESS_DENIED).
     */
    public static Long validateAndAssignTenantForPersist(Long currentEntityTenantId, String entityName) {
        Long activeTenantId = getOptionalTenantId().orElse(null);
        if (activeTenantId == null) {
            throw new DomainException(
                    "Explicit tenant context is required to persist " + entityName + ".",
                    HttpStatus.BAD_REQUEST, "TENANT_CONTEXT_REQUIRED"
            );
        }
        if (currentEntityTenantId == null) {
            return activeTenantId;
        }
        if (!currentEntityTenantId.equals(activeTenantId)) {
            throw new DomainException(
                    "Mismatched tenant ID: entity tenant (" + currentEntityTenantId + ") does not match authenticated tenant (" + activeTenantId + ").",
                    HttpStatus.FORBIDDEN, "TENANT_ACCESS_DENIED"
            );
        }
        return currentEntityTenantId;
    }

    /**
     * Validates tenant ID for entity updates (@PreUpdate).
     * Rejects missing tenant context with 400 (TENANT_CONTEXT_REQUIRED).
     * Rejects tenant reassignment attempts with 403 (TENANT_ACCESS_DENIED).
     */
    public static void validateTenantForUpdate(Long currentEntityTenantId, String entityName) {
        Long activeTenantId = getOptionalTenantId().orElse(null);
        if (activeTenantId == null) {
            throw new DomainException(
                    "Explicit tenant context is required to update " + entityName + ".",
                    HttpStatus.BAD_REQUEST, "TENANT_CONTEXT_REQUIRED"
            );
        }
        if (currentEntityTenantId != null && !currentEntityTenantId.equals(activeTenantId)) {
            throw new DomainException(
                    "Cannot reassign " + entityName + " to a different tenant.",
                    HttpStatus.FORBIDDEN, "TENANT_ACCESS_DENIED"
            );
        }
    }
}
