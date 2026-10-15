package com.fleetx.fleet.repository;

import com.fleetx.fleet.entity.ServiceWarning;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ServiceWarningRepository extends JpaRepository<ServiceWarning, Long> {

    List<ServiceWarning> findByVehicleIdAndResolvedFalse(Long vehicleId);

    List<ServiceWarning> findByResolvedFalseOrderByCreatedAtDesc();

    boolean existsByVehicleIdAndWarningTypeAndResolvedFalse(Long vehicleId, String warningType);

    boolean existsByVehicleIdAndSeverityAndResolvedFalse(Long vehicleId, String severity);
}
