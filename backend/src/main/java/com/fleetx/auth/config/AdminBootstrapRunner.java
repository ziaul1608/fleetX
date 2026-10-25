package com.fleetx.auth.config;

import com.fleetx.auth.entity.Role;
import com.fleetx.auth.entity.User;
import com.fleetx.auth.repository.RoleRepository;
import com.fleetx.auth.repository.UserRepository;
import com.fleetx.auth.service.AuditService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Component
public class AdminBootstrapRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrapRunner.class);

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;

    @Value("${fleetx.bootstrap.admin.enabled:false}")
    private boolean bootstrapEnabled;

    @Value("${fleetx.bootstrap.admin.email:admin@fleetx.local}")
    private String adminEmail;

    @Value("${fleetx.bootstrap.admin.password:}")
    private String adminPassword;

    @Value("${fleetx.bootstrap.admin.full-name:FleetX Administrator}")
    private String adminFullName;

    @Value("${fleetx.bootstrap.admin.password-override:false}")
    private boolean passwordOverride;

    public AdminBootstrapRunner(
            UserRepository userRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder,
            AuditService auditService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditService = auditService;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!bootstrapEnabled) {
            log.info("[AdminBootstrap] Automatic admin bootstrap is disabled (fleetx.bootstrap.admin.enabled=false).");
            return;
        }

        if (adminPassword == null || adminPassword.isBlank()) {
            log.warn("[AdminBootstrap] Admin bootstrap enabled but BOOTSTRAP_ADMIN_PASSWORD is empty. Skipping initial admin provisioning.");
            return;
        }

        String normalizedEmail = adminEmail.trim().toLowerCase();
        java.util.Optional<User> existingUserOpt = userRepository.findByEmail(normalizedEmail);

        if (passwordOverride) {
            log.warn("[AdminBootstrap] BOOTSTRAP_ADMIN_PASSWORD_OVERRIDE is retired and disabled. FleetX uses passwordless Email OTP authentication.");
            return;
        }

        // When passwordOverride is false:
        if (existingUserOpt.isPresent()) {
            log.info("[AdminBootstrap] Administrator account already exists: {}. Skipping provisioning.", normalizedEmail);
            return;
        }

        if (userRepository.countActiveUsersWithRole("ROLE_ADMIN") > 0) {
            log.info("[AdminBootstrap] Active administrator accounts already exist in system. Skipping initial admin provisioning to prevent duplicates.");
            return;
        }

        Role adminRole = roleRepository.findByName("ROLE_ADMIN")
                .orElseGet(() -> roleRepository.save(new Role("ROLE_ADMIN", "System Administrator with full access")));

        User adminUser = new User(
                normalizedEmail,
                passwordEncoder.encode(adminPassword.trim()),
                adminFullName.trim()
        );
        adminUser.setActive(true);
        adminUser.setRoles(Set.of(adminRole));

        User saved = userRepository.save(adminUser);
        auditService.recordEvent(
                saved.getId(),
                "ADMIN_BOOTSTRAP_INITIALIZED",
                "USER",
                saved.getId(),
                "127.0.0.1",
                "{\"bootstrap\":true,\"email\":\"" + normalizedEmail + "\"}"
        );

        log.info("[AdminBootstrap] Successfully bootstrapped initial administrator: {} (Role: ROLE_ADMIN)", normalizedEmail);
    }
}
