package com.fleetx.auth.service;

import com.fleetx.auth.dto.*;
import com.fleetx.auth.entity.Role;
import com.fleetx.auth.entity.User;
import com.fleetx.auth.repository.RoleRepository;
import com.fleetx.auth.repository.UserRepository;
import com.fleetx.shared.dto.PageResponse;
import com.fleetx.shared.exception.DomainException;
import com.fleetx.shared.exception.ResourceConflictException;
import com.fleetx.shared.exception.ResourceNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class AdminUserService {

    private static final Set<String> ALLOWED_ROLES = Set.of(
            "ROLE_ADMIN",
            "ROLE_FLEET_MANAGER",
            "ROLE_DISPATCHER",
            "ROLE_DRIVER",
            "ROLE_VIEWER"
    );

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;
    private final com.fleetx.auth.repository.TenantRepository tenantRepository;

    public AdminUserService(
            UserRepository userRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder,
            AuditService auditService,
            com.fleetx.auth.repository.TenantRepository tenantRepository) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditService = auditService;
        this.tenantRepository = tenantRepository;
    }

    @Transactional(readOnly = true)
    public PageResponse<UserResponseDto> getUsers(String role, Boolean active, String search, Pageable pageable) {
        Long tenantId = com.fleetx.shared.context.TenantSecurityContext.getCurrentTenantId();
        String roleFilter = null;
        if (role != null && !role.isBlank()) {
            roleFilter = role.trim().toUpperCase();
            if (!roleFilter.startsWith("ROLE_")) {
                roleFilter = "ROLE_" + roleFilter;
            }
        }

        String searchFilter = (search != null && !search.isBlank()) ? search.trim() : null;

        Page<User> page = userRepository.findWithFiltersByTenant(tenantId, roleFilter, active, searchFilter, pageable);

        List<UserResponseDto> content = page.getContent().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());

        return new PageResponse<>(
                content,
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isLast()
        );
    }

    @Transactional(readOnly = true)
    public UserResponseDto getUserById(Long id) {
        Long tenantId = com.fleetx.shared.context.TenantSecurityContext.getCurrentTenantId();
        User user = userRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));
        return mapToDto(user);
    }

    @Transactional
    public UserResponseDto createUser(UserCreateRequest request, HttpServletRequest httpRequest, Long performingAdminId) {
        Long tenantId = com.fleetx.shared.context.TenantSecurityContext.getCurrentTenantId();
        String normalizedEmail = request.email().trim().toLowerCase();
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new ResourceConflictException("A user account with email '" + normalizedEmail + "' already exists.");
        }

        Set<Role> roles = resolveRoles(request.roles());

        String passwordVal = request.password() != null && !request.password().isBlank()
                ? passwordEncoder.encode(request.password())
                : null;

        User user = new User(
                tenantId,
                normalizedEmail,
                passwordVal,
                request.fullName().trim()
        );
        user.setActive(true);
        user.setRoles(roles);

        User savedUser = userRepository.save(user);

        String clientIp = extractClientIp(httpRequest);
        auditService.recordEvent(
                performingAdminId,
                "USER_CREATED",
                "USER",
                savedUser.getId(),
                clientIp,
                "{\"email\":\"" + savedUser.getEmail() + "\",\"roles\":\"" + request.roles() + "\",\"tenantId\":" + tenantId + "}"
        );

        return mapToDto(savedUser);
    }

    @Transactional
    public UserResponseDto updateUser(Long id, UserUpdateRequest request, String currentAdminEmail, HttpServletRequest httpRequest, Long performingAdminId) {
        Long tenantId = com.fleetx.shared.context.TenantSecurityContext.getCurrentTenantId();
        User user = userRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));

        boolean isSelf = user.getEmail().equalsIgnoreCase(currentAdminEmail);

        Set<Role> targetRoles = resolveRoles(request.roles());
        boolean targetHasAdminRole = targetRoles.stream().anyMatch(r -> "ROLE_ADMIN".equals(r.getName()));
        boolean isCurrentAdmin = user.getRoles().stream().anyMatch(r -> "ROLE_ADMIN".equals(r.getName()));
        boolean isCurrentActive = user.isActive();

        // Prevent self-deactivation via update endpoint
        if (isSelf && request.active() != null && !request.active()) {
            throw new DomainException(
                    "Administrators cannot deactivate their own account.",
                    HttpStatus.BAD_REQUEST,
                    "SELF_DEACTIVATION_PREVENTED"
            );
        }

        // Prevent deactivation of sole active administrator within the tenant
        if (request.active() != null && !request.active() && isCurrentAdmin && isCurrentActive) {
            long activeAdminCount = userRepository.countActiveUsersWithRoleAndTenantId("ROLE_ADMIN", tenantId);
            if (activeAdminCount <= 1) {
                throw new DomainException(
                        "Cannot deactivate the sole remaining active administrator.",
                        HttpStatus.BAD_REQUEST,
                        "LAST_ADMIN_DEACTIVATION_PREVENTED"
                );
            }
        }

        // Prevent demotion of sole active administrator within the tenant
        if (isCurrentAdmin && isCurrentActive && !targetHasAdminRole) {
            long activeAdminCount = userRepository.countActiveUsersWithRoleAndTenantId("ROLE_ADMIN", tenantId);
            if (activeAdminCount <= 1) {
                throw new DomainException(
                        "Cannot remove ROLE_ADMIN from account because it is the sole active administrator.",
                        HttpStatus.BAD_REQUEST,
                        "PRIVILEGE_DEMOTION_PREVENTED"
                );
            }
        }

        user.setFullName(request.fullName().trim());
        user.setRoles(targetRoles);
        if (request.active() != null) {
            user.setActive(request.active());
        }

        User updatedUser = userRepository.save(user);

        String clientIp = extractClientIp(httpRequest);
        auditService.recordEvent(
                performingAdminId,
                "USER_UPDATED",
                "USER",
                updatedUser.getId(),
                clientIp,
                "{\"updatedRoles\":\"" + request.roles() + "\",\"active\":" + updatedUser.isActive() + "}"
        );

        return mapToDto(updatedUser);
    }

    @Transactional
    public UserResponseDto updateUserStatus(Long id, UserStatusUpdateRequest request, String currentAdminEmail, HttpServletRequest httpRequest, Long performingAdminId) {
        Long tenantId = com.fleetx.shared.context.TenantSecurityContext.getCurrentTenantId();
        User user = userRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));

        boolean isSelf = user.getEmail().equalsIgnoreCase(currentAdminEmail);

        if (!request.active()) {
            if (isSelf) {
                throw new DomainException(
                        "Administrators cannot deactivate their own active account.",
                        HttpStatus.BAD_REQUEST,
                        "SELF_DEACTIVATION_PREVENTED"
                );
            }

            boolean isUserAdmin = user.getRoles().stream().anyMatch(r -> "ROLE_ADMIN".equals(r.getName()));
            if (isUserAdmin && user.isActive()) {
                long activeAdminCount = userRepository.countActiveUsersWithRoleAndTenantId("ROLE_ADMIN", tenantId);
                if (activeAdminCount <= 1) {
                    throw new DomainException(
                            "Cannot deactivate the sole remaining active administrator.",
                            HttpStatus.BAD_REQUEST,
                            "LAST_ADMIN_DEACTIVATION_PREVENTED"
                    );
                }
            }
        }

        user.setActive(request.active());
        User savedUser = userRepository.save(user);

        String action = request.active() ? "USER_ACTIVATED" : "USER_DEACTIVATED";
        String clientIp = extractClientIp(httpRequest);
        auditService.recordEvent(
                performingAdminId,
                action,
                "USER",
                savedUser.getId(),
                clientIp,
                "{\"reason\":\"" + (request.reason() != null ? request.reason() : "") + "\"}"
        );

        return mapToDto(savedUser);
    }

    @Transactional
    public void resetUserPassword(Long id, AdminPasswordResetRequest request, HttpServletRequest httpRequest, Long performingAdminId) {
        throw new DomainException(
                "Password management has been retired. Users authenticate securely via Email OTP dispatched to their email.",
                HttpStatus.BAD_REQUEST,
                "PASSWORD_AUTH_RETIRED"
        );
    }

    private Set<Role> resolveRoles(Set<String> roleNames) {
        if (roleNames == null || roleNames.isEmpty()) {
            throw new DomainException("At least one role must be provided.", HttpStatus.BAD_REQUEST, "INVALID_ROLE");
        }

        Set<Role> resolvedRoles = new HashSet<>();
        for (String rawName : roleNames) {
            String normalized = rawName.trim().toUpperCase();
            if (!normalized.startsWith("ROLE_")) {
                normalized = "ROLE_" + normalized;
            }

            final String roleName = normalized;
            if (!ALLOWED_ROLES.contains(roleName)) {
                throw new DomainException(
                        "Role '" + rawName + "' is invalid. Permitted roles are: ADMIN, FLEET_MANAGER, DISPATCHER, DRIVER, VIEWER.",
                        HttpStatus.BAD_REQUEST,
                        "INVALID_ROLE"
                );
            }

            Role role = roleRepository.findByName(roleName)
                    .orElseGet(() -> roleRepository.save(new Role(roleName, "Auto-created system role")));
            resolvedRoles.add(role);
        }

        return resolvedRoles;
    }

    private UserResponseDto mapToDto(User user) {
        List<String> roleNames = user.getRoles().stream()
                .map(r -> r.getName().replace("ROLE_", ""))
                .collect(Collectors.toList());

        Long tid = user.getTenantId() != null ? user.getTenantId() : 1L;
        String companyName = "FleetX Global Logistics";
        String warehouseName = "Central Warehouse";
        if (user.getTenantId() != null) {
            var tenantOpt = tenantRepository.findById(user.getTenantId());
            if (tenantOpt.isPresent()) {
                companyName = tenantOpt.get().getCompanyName();
                warehouseName = tenantOpt.get().getWarehouseName();
            }
        }

        return new UserResponseDto(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                roleNames,
                user.isActive(),
                user.getCreatedAt(),
                tid,
                companyName,
                warehouseName
        );
    }

    private String extractClientIp(HttpServletRequest request) {
        if (request == null) return "127.0.0.1";
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        } else if (ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
