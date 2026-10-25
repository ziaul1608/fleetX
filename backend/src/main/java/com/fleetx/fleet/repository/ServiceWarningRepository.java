package com.fleetx.fleet.repository;

import com.fleetx.fleet.entity.ServiceWarning;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ServiceWarningRepository extends JpaRepository<ServiceWarning, Long> {

    List<ServiceWarning> findByVehicleIdAndResolvedFalse(Long vehicleId);

    List<ServiceWarning> findByResolvedFalseOrderByCreatedAtDesc();

    List<ServiceWarning> findByTenantIdAndResolvedFalseOrderByCreatedAtDesc(Long tenantId);

    Optional<ServiceWarning> findByIdAndTenantId(Long id, Long tenantId);

    boolean existsByVehicleIdAndWarningTypeAndResolvedFalse(Long vehicleId, String warningType);

    boolean existsByVehicleIdAndSeverityAndResolvedFalse(Long vehicleId, String severity);
}
