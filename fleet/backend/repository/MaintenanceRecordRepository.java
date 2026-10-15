package com.fleetx.fleet.repository;

import com.fleetx.fleet.entity.MaintenanceRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MaintenanceRecordRepository extends JpaRepository<MaintenanceRecord, Long> {

    List<MaintenanceRecord> findByVehicleIdOrderByServiceDateDesc(Long vehicleId);

    Page<MaintenanceRecord> findByVehicleIdOrderByServiceDateDesc(Long vehicleId, Pageable pageable);

    Page<MaintenanceRecord> findAllByOrderByServiceDateDesc(Pageable pageable);

    Optional<MaintenanceRecord> findTopByVehicleIdAndNextServiceDueKmIsNotNullOrderByServiceDateDesc(Long vehicleId);

    Optional<MaintenanceRecord> findTopByVehicleIdAndNextServiceDueDateIsNotNullOrderByServiceDateDesc(Long vehicleId);
}
