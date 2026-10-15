package com.fleetx.fleet.repository;

import com.fleetx.fleet.entity.FuelRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FuelRecordRepository extends JpaRepository<FuelRecord, Long> {

    List<FuelRecord> findByVehicleIdOrderByOdometerAtFuelingAsc(Long vehicleId);

    Page<FuelRecord> findByVehicleIdOrderByFuelDateDesc(Long vehicleId, Pageable pageable);

    Optional<FuelRecord> findTopByVehicleIdOrderByOdometerAtFuelingDescFuelDateDesc(Long vehicleId);

    long countByVehicleId(Long vehicleId);
}
