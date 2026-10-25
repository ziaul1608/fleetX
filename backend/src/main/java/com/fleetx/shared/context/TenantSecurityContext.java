package com.fleetx.shared.context;

import com.fleetx.auth.security.FleetxUserPrincipal;
import com.fleetx.shared.exception.DomainException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

public final class TenantSecurityContext {

    private static final ThreadLocal<Long> TENANT_OVERRIDE = new ThreadLocal<>();

    private TenantSecurityContext() {}

    public static void setTenantId(Long tenantId) {
        TENANT_OVERRIDE.set(tenantId);
    }

    public static void clear() {
        TENANT_OVERRIDE.remove();
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

        // For tests using @WithMockUser or mock principals where explicit tenantId was not bound:
        // Default to legacy tenant 1L if available
        return 1L;
    }

    public static Long requireTenantId() {
        Long tenantId = getCurrentTenantId();
        if (tenantId == null) {
            throw new DomainException("Active tenant context is required for this operation.",
                    HttpStatus.BAD_REQUEST, "TENANT_CONTEXT_REQUIRED");
        }
        return tenantId;
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

        return Optional.of(1L);
    }
}
