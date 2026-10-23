package com.fleetx.auth.service;

import com.fleetx.auth.dto.*;
import com.fleetx.auth.entity.Role;
import com.fleetx.auth.entity.Tenant;
import com.fleetx.auth.entity.User;
import com.fleetx.auth.repository.RoleRepository;
import com.fleetx.auth.repository.TenantRepository;
import com.fleetx.auth.repository.UserRepository;
import com.fleetx.auth.security.FleetxUserPrincipal;
import com.fleetx.shared.exception.DomainException;
import com.fleetx.shared.exception.ResourceNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;
    private final TenantRepository tenantRepository;
    private final RoleRepository roleRepository;
    private final OtpService otpService;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            AuditService auditService,
            TenantRepository tenantRepository,
            RoleRepository roleRepository,
            OtpService otpService
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditService = auditService;
        this.tenantRepository = tenantRepository;
        this.roleRepository = roleRepository;
        this.otpService = otpService;
    }

    @Transactional
    public OtpSentResponse sendSignupOtp(SendSignupOtpRequest request, HttpServletRequest httpRequest) {
        String normalizedEmail = request.email().trim().toLowerCase();
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new DomainException("An account with email '" + normalizedEmail + "' already exists. Please log in.",
                    HttpStatus.CONFLICT, "EMAIL_ALREADY_EXISTS");
        }
        String clientIp = extractClientIp(httpRequest);
        OtpSentResponse response = otpService.sendSignupOtp(request, clientIp);
        auditService.recordEvent(null, "SIGNUP_OTP_REQUESTED", "AUTH", null, clientIp,
                "{\"email\":\"" + normalizedEmail + "\"}");
        return response;
    }

    @Transactional
    public UserResponseDto verifySignupOtp(VerifySignupOtpRequest request, HttpServletRequest httpRequest) {
        String normalizedEmail = request.email().trim().toLowerCase();
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new DomainException("An account with email '" + normalizedEmail + "' already exists.",
                    HttpStatus.CONFLICT, "EMAIL_ALREADY_EXISTS");
        }

        otpService.verifyOtp(normalizedEmail, "SIGNUP", request.otp());

        // 1. Create new Tenant
        String tenantCode = "TNT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        Tenant tenant = new Tenant(tenantCode, request.companyName().trim(), request.warehouseName().trim());
        tenant = tenantRepository.save(tenant);

        // 2. Create Primary Admin User
        User user = new User();
        user.setTenantId(tenant.getId());
        user.setEmail(normalizedEmail);
        user.setFullName(request.fullName().trim());
        user.setActive(true);

        Role adminRole = roleRepository.findByName("ROLE_ADMIN").orElseGet(() -> {
            Role r = new Role("ROLE_ADMIN", "Tenant Administrator");
            return roleRepository.save(r);
        });
        user.getRoles().add(adminRole);
        user = userRepository.save(user);

        // 3. Establish Spring Security Session Authentication
        establishSecuritySession(user, tenant, httpRequest);

        String clientIp = extractClientIp(httpRequest);
        auditService.recordEvent(user.getId(), "TENANT_SIGNUP_SUCCESS", "TENANT", tenant.getId(), clientIp,
                "{\"tenantCode\":\"" + tenantCode + "\",\"companyName\":\"" + tenant.getCompanyName() + "\"}");

        return toUserResponseDto(user);
    }

    @Transactional
    public OtpSentResponse sendLoginOtp(SendLoginOtpRequest request, HttpServletRequest httpRequest) {
        String normalizedEmail = request.email().trim().toLowerCase();
        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new DomainException("No account registered with email '" + normalizedEmail + "'. Please sign up first.",
                        HttpStatus.NOT_FOUND, "USER_NOT_FOUND"));

        if (!user.isActive()) {
            throw new DomainException("Account has been deactivated. Please contact support.",
                    HttpStatus.FORBIDDEN, "ACCOUNT_DEACTIVATED");
        }

        String clientIp = extractClientIp(httpRequest);
        OtpSentResponse response = otpService.sendLoginOtp(request, clientIp, user.getFullName());
        auditService.recordEvent(user.getId(), "LOGIN_OTP_REQUESTED", "AUTH", user.getId(), clientIp,
                "{\"email\":\"" + normalizedEmail + "\"}");
        return response;
    }

    @Transactional
    public UserResponseDto verifyLoginOtp(VerifyLoginOtpRequest request, HttpServletRequest httpRequest) {
        String normalizedEmail = request.email().trim().toLowerCase();
        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User with email '" + normalizedEmail + "' not found."));

        if (!user.isActive()) {
            throw new DomainException("Account has been deactivated.", HttpStatus.FORBIDDEN, "ACCOUNT_DEACTIVATED");
        }

        otpService.verifyOtp(normalizedEmail, "LOGIN", request.otp());

        Tenant tenant = user.getTenantId() != null
                ? tenantRepository.findById(user.getTenantId()).orElse(null)
                : null;
        if (tenant != null && !"ACTIVE".equalsIgnoreCase(tenant.getStatus())) {
            throw new DomainException("Organization account is suspended.", HttpStatus.FORBIDDEN, "TENANT_INACTIVE");
        }

        establishSecuritySession(user, tenant, httpRequest);

        String clientIp = extractClientIp(httpRequest);
        auditService.recordEvent(user.getId(), "USER_OTP_LOGIN_SUCCESS", "USER", user.getId(), clientIp,
                "{\"sessionId\":\"" + httpRequest.getSession().getId() + "\"}");

        return toUserResponseDto(user);
    }

    @Transactional
    public UserResponseDto login(LoginRequest request, HttpServletRequest httpRequest) {
        String normalizedEmail = request.email().trim().toLowerCase();
        String clientIp = extractClientIp(httpRequest);

        Optional<User> userOpt = userRepository.findByEmail(normalizedEmail);
        if (userOpt.isEmpty()) {
            auditService.recordEvent(null, "USER_LOGIN_FAILED", "USER", null, clientIp,
                    "{\"email\":\"" + normalizedEmail + "\",\"reason\":\"USER_NOT_FOUND\"}");
            throw new BadCredentialsException("Invalid email or password credentials.");
        }

        User user = userOpt.get();

        if (user.getPasswordHash() == null || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            auditService.recordEvent(user.getId(), "USER_LOGIN_FAILED", "USER", user.getId(), clientIp,
                    "{\"email\":\"" + normalizedEmail + "\",\"reason\":\"BAD_CREDENTIALS\"}");
            throw new BadCredentialsException("Invalid email or password credentials.");
        }

        if (!user.isActive()) {
            auditService.recordEvent(user.getId(), "USER_LOGIN_BLOCKED", "USER", user.getId(), clientIp,
                    "{\"email\":\"" + normalizedEmail + "\",\"reason\":\"ACCOUNT_DEACTIVATED\"}");
            throw new DomainException(
                    "Account has been deactivated. Please contact your system administrator.",
                    HttpStatus.FORBIDDEN,
                    "ACCOUNT_DEACTIVATED"
            );
        }

        Tenant tenant = user.getTenantId() != null
                ? tenantRepository.findById(user.getTenantId()).orElse(null)
                : null;
        if (tenant != null && !"ACTIVE".equalsIgnoreCase(tenant.getStatus())) {
            throw new DomainException("Organization account is suspended.", HttpStatus.FORBIDDEN, "TENANT_INACTIVE");
        }

        establishSecuritySession(user, tenant, httpRequest);

        auditService.recordEvent(user.getId(), "USER_LOGIN_SUCCESS", "USER", user.getId(), clientIp,
                "{\"sessionId\":\"" + httpRequest.getSession().getId() + "\"}");

        return toUserResponseDto(user);
    }

    private void establishSecuritySession(User user, Tenant tenant, HttpServletRequest httpRequest) {
        FleetxUserPrincipal principal = new FleetxUserPrincipal(user, tenant);
        UsernamePasswordAuthenticationToken authToken =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authToken);
        SecurityContextHolder.setContext(context);

        HttpSession session = httpRequest.getSession(true);
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);
    }

    private UserResponseDto toUserResponseDto(User user) {
        List<String> roleNames = user.getRoles().stream()
                .map(Role::getName)
                .map(r -> r.replace("ROLE_", ""))
                .collect(Collectors.toList());

        Tenant tenant = user.getTenantId() != null
                ? tenantRepository.findById(user.getTenantId()).orElse(null)
                : null;
        String companyName = tenant != null ? tenant.getCompanyName() : "FleetX Global Logistics";
        String warehouseName = tenant != null ? tenant.getWarehouseName() : "Central Warehouse";

        return new UserResponseDto(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                roleNames,
                user.isActive(),
                user.getCreatedAt(),
                user.getTenantId() != null ? user.getTenantId() : 1L,
                companyName,
                warehouseName
        );
    }

    @Transactional(readOnly = true)
    public UserResponseDto getCurrentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new BadCredentialsException("No authenticated session found.");
        }

        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User with email '" + email + "' not found."));

        if (!user.isActive()) {
            throw new DomainException(
                    "Account has been deactivated. Access revoked.",
                    HttpStatus.FORBIDDEN,
                    "ACCOUNT_DEACTIVATED"
            );
        }

        return toUserResponseDto(user);
    }

    @Transactional
    public void changePassword(PasswordChangeRequest request, Authentication authentication, HttpServletRequest httpRequest) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new BadCredentialsException("No authenticated session found.");
        }

        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User with email '" + email + "' not found."));

        if (!user.isActive()) {
            throw new DomainException(
                    "Account has been deactivated. Access revoked.",
                    HttpStatus.FORBIDDEN,
                    "ACCOUNT_DEACTIVATED"
            );
        }

        if (!request.newPassword().equals(request.confirmPassword())) {
            throw new DomainException(
                    "New password and confirm password do not match.",
                    HttpStatus.BAD_REQUEST,
                    "PASSWORD_MISMATCH"
            );
        }

        String clientIp = extractClientIp(httpRequest);

        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            auditService.recordEvent(
                    user.getId(),
                    "USER_PASSWORD_CHANGE_FAILED",
                    "USER",
                    user.getId(),
                    clientIp,
                    "{\"email\":\"" + user.getEmail() + "\",\"reason\":\"INVALID_CURRENT_PASSWORD\"}"
            );
            throw new DomainException(
                    "Current password is incorrect.",
                    HttpStatus.BAD_REQUEST,
                    "INVALID_CURRENT_PASSWORD"
            );
        }

        if (passwordEncoder.matches(request.newPassword(), user.getPasswordHash())) {
            throw new DomainException(
                    "New password cannot be identical to current password.",
                    HttpStatus.BAD_REQUEST,
                    "PASSWORD_REUSE_PREVENTED"
            );
        }

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);

        auditService.recordEvent(
                user.getId(),
                "USER_PASSWORD_CHANGED",
                "USER",
                user.getId(),
                clientIp,
                "{\"email\":\"" + user.getEmail() + "\"}"
        );
    }

    public void logout(HttpServletRequest request, Authentication authentication) {
        Long userId = null;
        if (authentication != null && authentication.isAuthenticated()) {
            userRepository.findByEmail(authentication.getName()).ifPresent(u -> {
                auditService.recordEvent(u.getId(), "USER_LOGOUT", "USER", u.getId(), extractClientIp(request), "{}");
            });
        }

        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        SecurityContextHolder.clearContext();
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
