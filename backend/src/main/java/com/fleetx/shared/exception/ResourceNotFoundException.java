package com.fleetx.shared.exception;

import org.springframework.http.HttpStatus;

public class ResourceNotFoundException extends DomainException {

    public ResourceNotFoundException(String message) {
        super(message, HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND");
    }

    public ResourceNotFoundException(String entityName, Object id) {
        super(entityName + " with identifier '" + id + "' was not found.", HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND");
    }
}
