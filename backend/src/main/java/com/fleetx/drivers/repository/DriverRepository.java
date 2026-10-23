package com.fleetx.drivers.repository;

import com.fleetx.drivers.entity.Driver;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface DriverRepository extends JpaRepository<Driver, Long> {

    Optional<Driver> findByIdAndTenantId(Long id, Long tenantId);

    Optional<Driver> findByUserId(Long userId);

    Optional<Driver> findByLicenseNumber(String licenseNumber);

    Optional<Driver> findByTenantIdAndLicenseNumber(Long tenantId, String licenseNumber);

    boolean existsByLicenseNumber(String licenseNumber);

    boolean existsByTenantIdAndLicenseNumber(Long tenantId, String licenseNumber);

    boolean existsByLicenseNumberAndIdNot(String licenseNumber, Long id);

    boolean existsByTenantIdAndLicenseNumberAndIdNot(Long tenantId, String licenseNumber, Long id);

    List<Driver> findByAvailabilityStatus(String status);

    List<Driver> findByTenantIdAndAvailabilityStatus(Long tenantId, String status);

    List<Driver> findByActiveTrue();

    List<Driver> findByTenantId(Long tenantId);

    @Query("SELECT d FROM Driver d WHERE " +
            "d.tenantId = :tenantId AND " +
            "(:search IS NULL OR :search = '' OR " +
            " LOWER(d.fullName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            " LOWER(d.licenseNumber) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            " LOWER(d.phoneNumber) LIKE LOWER(CONCAT('%', :search, '%'))) " +
            "AND (:availability IS NULL OR :availability = '' OR d.availabilityStatus = :availability) " +
            "AND (:active IS NULL OR d.active = :active)")
    Page<Driver> findWithFilters(@Param("tenantId") Long tenantId,
                                 @Param("search") String search,
                                 @Param("availability") String availability,
                                 @Param("active") Boolean active,
                                 Pageable pageable);

    @Query("SELECT d FROM Driver d WHERE d.active = true AND d.licenseExpiryDate <= :thresholdDate")
    List<Driver> findDriversWithExpiringLicenses(@Param("thresholdDate") LocalDate thresholdDate);
}
