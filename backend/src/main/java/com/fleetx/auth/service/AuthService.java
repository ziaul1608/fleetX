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

        // 2. Create Primary Admin User inside explicit tenant context
        final Tenant savedTenant = tenant;
        User user = com.fleetx.shared.context.TenantSecurityContext.executeWithTenant(savedTenant.getId(), () -> {
            User u = new User();
            u.setTenantId(savedTenant.getId());
            u.setEmail(normalizedEmail);
            u.setFullName(request.fullName().trim());
            u.setActive(true);

            Role adminRole = roleRepository.findByName("ROLE_ADMIN").orElseGet(() -> {
                Role r = new Role("ROLE_ADMIN", "Tenant Administrator");
                return roleRepository.save(r);
            });
            u.getRoles().add(adminRole);
            return userRepository.save(u);
        });

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
        Optional<User> userOpt = userRepository.findByEmail(normalizedEmail);

        String clientIp = extractClientIp(httpRequest);

        if (userOpt.isEmpty() || !userOpt.get().isActive()) {
            auditService.recordEvent(null, "LOGIN_OTP_REQUESTED_UNKNOWN_OR_INACTIVE", "AUTH", null, clientIp,
                    "{\"email\":\"" + normalizedEmail + "\"}");
            return new OtpSentResponse(
                    normalizedEmail,
                    "LOGIN",
                    OtpService.VALIDITY_SECONDS,
                    OtpService.COOLDOWN_SECONDS,
                    "If an account is associated with this email address, a verification code has been dispatched."
            );
        }

        User user = userOpt.get();
        OtpSentResponse response = otpService.sendLoginOtp(request, clientIp, user.getFullName());
        auditService.recordEvent(user.getId(), "LOGIN_OTP_REQUESTED", "AUTH", user.getId(), clientIp,
                "{\"email\":\"" + normalizedEmail + "\"}");
        return response;
    }

    @Transactional
    public UserResponseDto verifyLoginOtp(VerifyLoginOtpRequest request, HttpServletRequest httpRequest) {
        String normalizedEmail = request.email().trim().toLowerCase();

        // 1. Verify OTP first (fails with INVALID_OTP_FORMAT, NO_ACTIVE_OTP, INCORRECT_OTP, OTP_EXPIRED, etc.)
        otpService.verifyOtp(normalizedEmail, "LOGIN", request.otp());

        // 2. Fetch User
        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new DomainException("Invalid credentials or account does not exist.",
                        HttpStatus.BAD_REQUEST, "INVALID_CREDENTIALS"));

        // 3. Verify Account Active Status
        if (!user.isActive()) {
            throw new DomainException("Account has been deactivated. Please contact support.",
                    HttpStatus.FORBIDDEN, "ACCOUNT_DEACTIVATED");
        }

        // 4. Verify Tenant Active Status
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
        throw new DomainException(
                "Password-only authentication has been retired. Please log in using your registered email and a 6-digit verification code.",
                HttpStatus.BAD_REQUEST,
                "PASSWORD_AUTH_RETIRED"
        );
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
        String companyName = tenant != null ? tenant.getCompanyName() : null;
        String warehouseName = tenant != null ? tenant.getWarehouseName() : null;

        return new UserResponseDto(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                roleNames,
                user.isActive(),
                user.getCreatedAt(),
                user.getTenantId(),
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
        throw new DomainException(
                "Password management has been retired. FleetX uses passwordless Email OTP authentication.",
                HttpStatus.BAD_REQUEST,
                "PASSWORD_AUTH_RETIRED"
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
