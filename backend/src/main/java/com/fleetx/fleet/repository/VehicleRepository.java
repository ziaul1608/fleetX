package com.fleetx.fleet.repository;

import com.fleetx.fleet.entity.Vehicle;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VehicleRepository extends JpaRepository<Vehicle, Long> {

    Optional<Vehicle> findByIdAndTenantId(Long id, Long tenantId);

    Optional<Vehicle> findByVin(String vin);

    Optional<Vehicle> findByVinAndTenantId(String vin, Long tenantId);

    Optional<Vehicle> findByLicensePlate(String licensePlate);

    Optional<Vehicle> findByLicensePlateAndTenantId(String licensePlate, Long tenantId);

    boolean existsByVin(String vin);

    boolean existsByVinAndTenantId(String vin, Long tenantId);

    boolean existsByLicensePlate(String licensePlate);

    boolean existsByLicensePlateAndTenantId(String licensePlate, Long tenantId);

    List<Vehicle> findByStatus(String status);

    List<Vehicle> findByTenantIdAndStatus(Long tenantId, String status);

    List<Vehicle> findByActiveTrue();

    List<Vehicle> findByTenantIdAndActiveTrue(Long tenantId);

    List<Vehicle> findByTenantId(Long tenantId);

    @Query("SELECT v FROM Vehicle v WHERE " +
           "v.tenantId = :tenantId AND " +
           "(:status IS NULL OR LOWER(v.status) = LOWER(:status)) AND " +
           "(:vehicleType IS NULL OR LOWER(v.vehicleType) = LOWER(:vehicleType)) AND " +
           "(:active IS NULL OR v.active = :active) AND " +
           "(:search IS NULL OR " +
           " LOWER(v.licensePlate) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           " LOWER(v.vin) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           " LOWER(v.make) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           " LOWER(v.model) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Vehicle> findWithFilters(
            @Param("tenantId") Long tenantId,
            @Param("status") String status,
            @Param("vehicleType") String vehicleType,
            @Param("active") Boolean active,
            @Param("search") String search,
            Pageable pageable
    );
}
