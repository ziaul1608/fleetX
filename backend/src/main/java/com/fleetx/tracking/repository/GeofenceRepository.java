package com.fleetx.tracking.repository;

import com.fleetx.tracking.entity.Geofence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GeofenceRepository extends JpaRepository<Geofence, Long> {

    Optional<Geofence> findByIdAndTenantId(Long id, Long tenantId);

    List<Geofence> findByTenantIdAndActiveTrueOrderByIdAsc(Long tenantId);

    List<Geofence> findByTenantIdOrderByIdAsc(Long tenantId);

    Optional<Geofence> findByTenantIdAndNameIgnoreCase(Long tenantId, String name);

    boolean existsByTenantIdAndNameIgnoreCase(Long tenantId, String name);

    boolean existsByNameIgnoreCase(String name);

    List<Geofence> findAllByOrderByIdAsc();

    List<Geofence> findByActiveTrueOrderByIdAsc();
}
