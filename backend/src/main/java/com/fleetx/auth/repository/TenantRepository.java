package com.fleetx.auth.repository;

import com.fleetx.auth.entity.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TenantRepository extends JpaRepository<Tenant, Long> {
    Optional<Tenant> findByTenantCode(String tenantCode);
    boolean existsByTenantCode(String tenantCode);
}
