package com.fleetx.auth.service;

import com.fleetx.auth.dto.LoginRequest;
import com.fleetx.auth.dto.UserResponseDto;
import com.fleetx.auth.entity.Role;
import com.fleetx.auth.entity.User;
import com.fleetx.auth.repository.UserRepository;
import com.fleetx.shared.exception.DomainException;
import com.fleetx.shared.exception.ResourceNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, AuditService auditService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditService = auditService;
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

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
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

        List<SimpleGrantedAuthority> authorities = user.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority(role.getName()))
                .collect(Collectors.toList());

        UsernamePasswordAuthenticationToken authToken =
                new UsernamePasswordAuthenticationToken(user.getEmail(), null, authorities);

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authToken);
        SecurityContextHolder.setContext(context);

        HttpSession session = httpRequest.getSession(true);
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);

        auditService.recordEvent(user.getId(), "USER_LOGIN_SUCCESS", "USER", user.getId(), clientIp,
                "{\"sessionId\":\"" + session.getId() + "\"}");

        List<String> roleNames = user.getRoles().stream()
                .map(Role::getName)
                .map(r -> r.replace("ROLE_", ""))
                .collect(Collectors.toList());

        return new UserResponseDto(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                roleNames,
                user.isActive(),
                user.getCreatedAt()
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

        List<String> roleNames = user.getRoles().stream()
                .map(Role::getName)
                .map(r -> r.replace("ROLE_", ""))
                .collect(Collectors.toList());

        return new UserResponseDto(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                roleNames,
                user.isActive(),
                user.getCreatedAt()
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
