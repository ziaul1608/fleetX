package com.fleetx.tracking.repository;

import com.fleetx.tracking.entity.ControlTowerKpi;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface ControlTowerKpiRepository extends JpaRepository<ControlTowerKpi, Long> {

    Optional<ControlTowerKpi> findBySnapshotDate(LocalDate snapshotDate);
}
