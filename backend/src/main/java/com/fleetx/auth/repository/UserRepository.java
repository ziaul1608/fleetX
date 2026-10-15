package com.fleetx.auth.repository;

import com.fleetx.auth.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);

    @Query("SELECT COUNT(u) FROM User u JOIN u.roles r WHERE r.name = :roleName AND u.active = true")
    long countActiveUsersWithRole(@Param("roleName") String roleName);

    Page<User> findByRoles_Name(String roleName, Pageable pageable);

    @Query("SELECT u FROM User u WHERE " +
           "(:role IS NULL OR EXISTS (SELECT r FROM u.roles r WHERE r.name = :role)) AND " +
           "(:active IS NULL OR u.active = :active) AND " +
           "(:search IS NULL OR LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(u.fullName) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<User> findWithFilters(
            @Param("role") String role,
            @Param("active") Boolean active,
            @Param("search") String search,
            Pageable pageable
    );
}
