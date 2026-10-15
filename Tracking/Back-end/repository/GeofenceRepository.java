package com.fleetx.tracking.repository;

import com.fleetx.tracking.entity.Geofence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GeofenceRepository extends JpaRepository<Geofence, Long> {

    List<Geofence> findByActiveTrueOrderByIdAsc();

    List<Geofence> findAllByOrderByIdAsc();

    Optional<Geofence> findByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCase(String name);
}
