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
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Component
@Order(10)
public class DemoIdentitySeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoIdentitySeeder.class);

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;

    @Value("${fleetx.demo.seed.enabled:false}")
    private boolean demoSeedEnabled;

    public DemoIdentitySeeder(
            UserRepository userRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder,
            AuditService auditService
    ) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditService = auditService;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!demoSeedEnabled) {
            return;
        }
        seedDemoUsers();
    }

    public void seedDemoUsers() {
        ensureRole("ROLE_ADMIN", "System Administrator with full access");
        Role fleetManagerRole = ensureRole("ROLE_FLEET_MANAGER", "Fleet & Asset Manager");
        Role dispatcherRole = ensureRole("ROLE_DISPATCHER", "Logistics Dispatcher");
        Role driverRole = ensureRole("ROLE_DRIVER", "Commercial Fleet Driver");
        Role viewerRole = ensureRole("ROLE_VIEWER", "Auditor & Executive Read-Only Viewer");

        ensureDemoUser("manager@fleetx.local", "DevManager123!", "Priya Nair (Fleet Manager)", fleetManagerRole);
        ensureDemoUser("dispatcher@fleetx.local", "DevDispatch123!", "Arjun Mehta (Lead Dispatcher)", dispatcherRole);
        ensureDemoUser("driver.rajesh@fleetx.local", "DevDriver123!", "Rajesh Sharma (Senior Driver)", driverRole);
        ensureDemoUser("viewer@fleetx.local", "DevViewer123!", "Dr. S. Kulkarni (PBL Evaluator)", viewerRole);

        log.info("[DemoIdentitySeeder] Repeatable development role and user accounts verified.");
    }

    private Role ensureRole(String name, String description) {
        return roleRepository.findByName(name)
                .orElseGet(() -> roleRepository.save(new Role(name, description)));
    }

    private void ensureDemoUser(String email, String rawPassword, String fullName, Role role) {
        if (userRepository.existsByEmail(email)) {
            return;
        }
        User user = new User(email, passwordEncoder.encode(rawPassword), fullName);
        user.setActive(true);
        user.setRoles(Set.of(role));
        User saved = userRepository.save(user);
        auditService.recordEvent(
                saved.getId(),
                "DEMO_USER_SEEDED",
                "USER",
                saved.getId(),
                "127.0.0.1",
                "{\"demoSeed\":true,\"email\":\"" + email + "\"}"
        );
    }
}
