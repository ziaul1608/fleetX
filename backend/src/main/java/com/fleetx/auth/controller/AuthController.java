package com.fleetx.auth.controller;

import com.fleetx.auth.dto.*;
import com.fleetx.auth.service.AuthService;
import com.fleetx.shared.dto.ApiResponse;
import com.fleetx.shared.exception.DomainException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
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

    @PostMapping("/otp/send-signup")
    public ResponseEntity<ApiResponse<OtpSentResponse>> sendSignupOtp(
            @Valid @RequestBody SendSignupOtpRequest request,
            HttpServletRequest httpRequest) {
        OtpSentResponse response = authService.sendSignupOtp(request, httpRequest);
        return ResponseEntity.ok(ApiResponse.ok("Verification code sent successfully.", response));
    }

    @PostMapping("/otp/verify-signup")
    public ResponseEntity<ApiResponse<UserResponseDto>> verifySignupOtp(
            @Valid @RequestBody VerifySignupOtpRequest request,
            HttpServletRequest httpRequest) {
        UserResponseDto userDto = authService.verifySignupOtp(request, httpRequest);
        return ResponseEntity.ok(ApiResponse.ok("Registration completed successfully. Welcome to FleetX!", userDto));
    }

    @PostMapping("/otp/send-login")
    public ResponseEntity<ApiResponse<OtpSentResponse>> sendLoginOtp(
            @Valid @RequestBody SendLoginOtpRequest request,
            HttpServletRequest httpRequest) {
        OtpSentResponse response = authService.sendLoginOtp(request, httpRequest);
        return ResponseEntity.ok(ApiResponse.ok("Verification code sent successfully.", response));
    }

    @PostMapping("/otp/verify-login")
    public ResponseEntity<ApiResponse<UserResponseDto>> verifyLoginOtp(
            @Valid @RequestBody VerifyLoginOtpRequest request,
            HttpServletRequest httpRequest) {
        UserResponseDto userDto = authService.verifyLoginOtp(request, httpRequest);
        return ResponseEntity.ok(ApiResponse.ok("Login successful. Welcome back!", userDto));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<UserResponseDto>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {
        throw new DomainException(
                "Password-only authentication has been retired. Please log in using your registered email and a 6-digit verification code.",
                HttpStatus.BAD_REQUEST,
                "PASSWORD_AUTH_RETIRED"
        );
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
            @Valid @RequestBody(required = false) PasswordChangeRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest) {
        throw new DomainException(
                "Password management has been retired. FleetX uses passwordless Email OTP authentication.",
                HttpStatus.BAD_REQUEST,
                "PASSWORD_AUTH_RETIRED"
        );
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
