package com.fleetx.auth.controller;

import com.fleetx.auth.dto.*;
import com.fleetx.auth.entity.User;
import com.fleetx.auth.repository.UserRepository;
import com.fleetx.auth.service.AdminUserService;
import com.fleetx.auth.service.AuditService;
import com.fleetx.shared.dto.ApiResponse;
import com.fleetx.shared.dto.PageResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping({"/api/v1/admin", "/api/admin"})
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final AdminUserService adminUserService;
    private final AuditService auditService;
    private final UserRepository userRepository;

    public AdminUserController(
            AdminUserService adminUserService,
            AuditService auditService,
            UserRepository userRepository) {
        this.adminUserService = adminUserService;
        this.auditService = auditService;
        this.userRepository = userRepository;
    }

    @GetMapping("/users")
    public ResponseEntity<ApiResponse<PageResponse<UserResponseDto>>> listUsers(
            @RequestParam(required = false) String role,
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort) {

        int clampedSize = Math.max(1, Math.min(size, 100));
        Sort sortOrder = Sort.by(Sort.Direction.DESC, "createdAt");
        if (sort != null && sort.contains(",")) {
            String[] parts = sort.split(",");
            Sort.Direction direction = parts[1].equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;
            sortOrder = Sort.by(direction, parts[0]);
        }

        Pageable pageable = PageRequest.of(page, clampedSize, sortOrder);
        PageResponse<UserResponseDto> users = adminUserService.getUsers(role, active, search, pageable);

        return ResponseEntity.ok(ApiResponse.ok("Users retrieved successfully.", users));
    }

    @GetMapping("/users/{id}")
    public ResponseEntity<ApiResponse<UserResponseDto>> getUserById(@PathVariable Long id) {
        UserResponseDto user = adminUserService.getUserById(id);
        return ResponseEntity.ok(ApiResponse.ok("User retrieved successfully.", user));
    }

    @PostMapping("/users")
    public ResponseEntity<ApiResponse<UserResponseDto>> createUser(
            @Valid @RequestBody UserCreateRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest) {

        Long adminId = resolveAdminId(authentication);
        UserResponseDto created = adminUserService.createUser(request, httpRequest, adminId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("User account provisioned successfully.", created));
    }

    @PutMapping("/users/{id}")
    public ResponseEntity<ApiResponse<UserResponseDto>> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UserUpdateRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest) {

        String currentAdminEmail = authentication.getName();
        Long adminId = resolveAdminId(authentication);
        UserResponseDto updated = adminUserService.updateUser(id, request, currentAdminEmail, httpRequest, adminId);
        return ResponseEntity.ok(ApiResponse.ok("User account updated successfully.", updated));
    }

    @RequestMapping(value = "/users/{id}/status", method = {RequestMethod.PUT, RequestMethod.PATCH})
    public ResponseEntity<ApiResponse<UserResponseDto>> updateUserStatus(
            @PathVariable Long id,
            @Valid @RequestBody UserStatusUpdateRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest) {

        String currentAdminEmail = authentication.getName();
        Long adminId = resolveAdminId(authentication);
        UserResponseDto updated = adminUserService.updateUserStatus(id, request, currentAdminEmail, httpRequest, adminId);
        String actionMsg = request.active() ? "User account activated successfully." : "User account deactivated successfully.";
        return ResponseEntity.ok(ApiResponse.ok(actionMsg, updated));
    }

    @RequestMapping(value = "/users/{id}/password-reset", method = {RequestMethod.PUT, RequestMethod.POST})
    public ResponseEntity<ApiResponse<Void>> resetPassword(
            @PathVariable Long id,
            @Valid @RequestBody AdminPasswordResetRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest) {

        Long adminId = resolveAdminId(authentication);
        adminUserService.resetUserPassword(id, request, httpRequest, adminId);
        return ResponseEntity.ok(ApiResponse.ok("Password reset successfully by administrator.", null));
    }

    @GetMapping("/audit-logs")
    public ResponseEntity<ApiResponse<PageResponse<AuditLogResponseDto>>> listAuditLogs(
            @RequestParam(required = false) String action,
            @RequestParam(required = false) Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        int clampedSize = Math.max(1, Math.min(size, 100));
        Pageable pageable = PageRequest.of(page, clampedSize);
        PageResponse<AuditLogResponseDto> logs = auditService.getAuditLogs(action, userId, pageable);
        return ResponseEntity.ok(ApiResponse.ok("Audit logs retrieved successfully.", logs));
    }

    private Long resolveAdminId(Authentication authentication) {
        if (authentication == null) return null;
        return userRepository.findByEmail(authentication.getName())
                .map(User::getId)
                .orElse(null);
    }
}
