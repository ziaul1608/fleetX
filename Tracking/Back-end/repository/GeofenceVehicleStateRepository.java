package com.fleetx.tracking.repository;

import com.fleetx.tracking.entity.GeofenceVehicleState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GeofenceVehicleStateRepository extends JpaRepository<GeofenceVehicleState, Long> {

    Optional<GeofenceVehicleState> findByGeofenceIdAndVehicleId(Long geofenceId, Long vehicleId);
}
