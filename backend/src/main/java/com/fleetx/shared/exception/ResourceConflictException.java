package com.fleetx.shared.exception;

import org.springframework.http.HttpStatus;

public class ResourceConflictException extends DomainException {

    public ResourceConflictException(String message) {
        super(message, HttpStatus.CONFLICT, "RESOURCE_CONFLICT");
    }
}
