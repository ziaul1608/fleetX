package com.fleetx.auth.controller;

import com.fleetx.auth.dto.LoginRequest;
import com.fleetx.auth.dto.PasswordChangeRequest;
import com.fleetx.auth.dto.UserResponseDto;
import com.fleetx.auth.service.AuthService;
import com.fleetx.shared.dto.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping({"/api/v1/auth", "/api/auth"})
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<UserResponseDto>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {
        UserResponseDto userDto = authService.login(request, httpRequest);
        return ResponseEntity.ok(ApiResponse.ok("Authentication successful.", userDto));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponseDto>> getCurrentUser(Authentication authentication) {
        UserResponseDto userDto = authService.getCurrentUser(authentication);
        return ResponseEntity.ok(ApiResponse.ok("Session retrieved.", userDto));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            HttpServletRequest httpRequest,
            Authentication authentication) {
        authService.logout(httpRequest, authentication);
        return ResponseEntity.ok(ApiResponse.ok("Logged out successfully.", null));
    }

    @PutMapping("/password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @Valid @RequestBody PasswordChangeRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest) {
        authService.changePassword(request, authentication, httpRequest);
        return ResponseEntity.ok(ApiResponse.ok("Password updated successfully.", null));
    }

    @GetMapping("/csrf")
    public ResponseEntity<ApiResponse<Map<String, String>>> getCsrfToken(CsrfToken token) {
        Map<String, String> data = new LinkedHashMap<>();
        if (token != null) {
            data.put("token", token.getToken());
            data.put("headerName", token.getHeaderName());
            data.put("parameterName", token.getParameterName());
        }
        return ResponseEntity.ok(ApiResponse.ok("CSRF token initialized.", data));
    }
}
