package com.fleetx.auth.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;

public class FleetxUserPrincipal implements UserDetails {

    private final Long userId;
    private final String email;
    private final Long tenantId;
    private final String companyName;
    private final String warehouseName;
    private final String passwordHash;
    private final boolean active;
    private final Collection<? extends GrantedAuthority> authorities;

    public FleetxUserPrincipal(
            Long userId,
            String email,
            Long tenantId,
            String companyName,
            String warehouseName,
            String passwordHash,
            boolean active,
            Collection<? extends GrantedAuthority> authorities) {
        this.userId = userId;
        this.email = email;
        this.tenantId = tenantId;
        this.companyName = companyName;
        this.warehouseName = warehouseName;
        this.passwordHash = passwordHash;
        this.active = active;
        this.authorities = authorities;
    }

    public FleetxUserPrincipal(com.fleetx.auth.entity.User user, com.fleetx.auth.entity.Tenant tenant) {
        this(
                user.getId(),
                user.getEmail(),
                user.getTenantId() != null ? user.getTenantId() : (tenant != null ? tenant.getId() : 1L),
                tenant != null ? tenant.getCompanyName() : "FleetX Global Logistics",
                tenant != null ? tenant.getWarehouseName() : "Central Warehouse",
                user.getPasswordHash(),
                user.isActive(),
                user.getRoles().stream()
                        .map(r -> new org.springframework.security.core.authority.SimpleGrantedAuthority(r.getName()))
                        .toList()
        );
    }

    public Long getUserId() { return userId; }
    public Long getTenantId() { return tenantId; }
    public String getCompanyName() { return companyName; }
    public String getWarehouseName() { return warehouseName; }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() { return authorities; }

    @Override
    public String getPassword() { return passwordHash != null ? passwordHash : ""; }

    @Override
    public String getUsername() { return email; }

    @Override
    public boolean isAccountNonExpired() { return true; }

    @Override
    public boolean isAccountNonLocked() { return active; }

    @Override
    public boolean isCredentialsNonExpired() { return true; }

    @Override
    public boolean isEnabled() { return active; }
}
