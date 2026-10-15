package com.fleetx.shared.controller;

import com.fleetx.shared.dto.ApiResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/health")
public class HealthController {

    private final Environment environment;

    @Value("${spring.application.name:fleetx-backend}")
    private String applicationName;

    public HealthController(Environment environment) {
        this.environment = environment;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> getHealthStatus() {
        Map<String, Object> healthInfo = new LinkedHashMap<>();
        healthInfo.put("status", "UP");
        healthInfo.put("application", applicationName);
        healthInfo.put("timestamp", Instant.now());
        healthInfo.put("activeProfiles", Arrays.asList(environment.getActiveProfiles()));
        healthInfo.put("version", "0.0.1-SNAPSHOT");

        return ResponseEntity.ok(ApiResponse.ok("FleetX Service is operating normally.", healthInfo));
    }
}
