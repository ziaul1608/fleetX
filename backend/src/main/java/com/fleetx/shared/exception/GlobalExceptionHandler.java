package com.fleetx.shared.exception;

import com.fleetx.shared.dto.ApiErrorResponse;
import com.fleetx.shared.dto.ValidationErrorDto;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.ArrayList;
import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(DomainException.class)
    public ResponseEntity<ApiErrorResponse> handleDomainException(DomainException ex, HttpServletRequest request) {
        log.warn("Domain exception caught [{}]: {}", ex.getErrorCode(), ex.getMessage());
        ApiErrorResponse error = ApiErrorResponse.of(
                ex.getStatus().value(),
                ex.getErrorCode(),
                ex.getMessage(),
                request.getRequestURI()
        );
        return ResponseEntity.status(ex.getStatus()).body(error);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidationException(MethodArgumentNotValidException ex, HttpServletRequest request) {
        List<ValidationErrorDto> validationErrors = new ArrayList<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            validationErrors.add(new ValidationErrorDto(
                    fieldError.getField(),
                    fieldError.getRejectedValue(),
                    fieldError.getDefaultMessage()
            ));
        }

        ApiErrorResponse error = ApiErrorResponse.of(
                HttpStatus.BAD_REQUEST.value(),
                "VALIDATION_FAILED",
                "One or more input fields failed validation constraints.",
                request.getRequestURI(),
                validationErrors
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiErrorResponse> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        log.warn("Access denied on {}: {}", request.getRequestURI(), ex.getMessage());
        ApiErrorResponse error = ApiErrorResponse.of(
                HttpStatus.FORBIDDEN.value(),
                "FORBIDDEN",
                "Access is denied. Required authorities are missing.",
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiErrorResponse> handleAuthenticationException(AuthenticationException ex, HttpServletRequest request) {
        log.warn("Authentication failed on {}: {}", request.getRequestURI(), ex.getMessage());
        ApiErrorResponse error = ApiErrorResponse.of(
                HttpStatus.UNAUTHORIZED.value(),
                "UNAUTHORIZED",
                "Authentication required or credentials invalid.",
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiErrorResponse> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {
        ApiErrorResponse error = ApiErrorResponse.of(
                HttpStatus.METHOD_NOT_ALLOWED.value(),
                "METHOD_NOT_ALLOWED",
                ex.getMessage(),
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(error);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNoResourceFound(NoResourceFoundException ex, HttpServletRequest request) {
        ApiErrorResponse error = ApiErrorResponse.of(
                HttpStatus.NOT_FOUND.value(),
                "RESOURCE_NOT_FOUND",
                "Endpoint or resource not found: " + request.getRequestURI(),
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleDataIntegrityViolation(DataIntegrityViolationException ex, HttpServletRequest request) {
        log.warn("Database integrity violation on {}: {}", request.getRequestURI(), ex.getMessage());
        String message = "A conflict occurred with an existing record. Please verify your details.";
        String errorCode = "DATA_CONFLICT";
        String msgLower = ex.getMessage() != null ? ex.getMessage().toLowerCase() : "";
        if (msgLower.contains("duplicate") || msgLower.contains("unique")) {
            if (msgLower.contains("email")) {
                message = "An account with this email address already exists.";
                errorCode = "EMAIL_ALREADY_EXISTS";
            } else {
                message = "A record with the specified unique values already exists.";
                errorCode = "DUPLICATE_RECORD";
            }
        }
        ApiErrorResponse error = ApiErrorResponse.of(
                HttpStatus.CONFLICT.value(),
                errorCode,
                message,
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGeneralException(Exception ex, HttpServletRequest request) {
        log.error("Unhandled server exception on " + request.getRequestURI(), ex);
        ApiErrorResponse error = ApiErrorResponse.of(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "INTERNAL_SERVER_ERROR",
                "An unexpected internal error occurred. Please contact system support.",
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}
