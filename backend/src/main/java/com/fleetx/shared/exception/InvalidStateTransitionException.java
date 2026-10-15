package com.fleetx.shared.exception;

import org.springframework.http.HttpStatus;

public class InvalidStateTransitionException extends DomainException {

    public InvalidStateTransitionException(String message) {
        super(message, HttpStatus.BAD_REQUEST, "INVALID_STATE_TRANSITION");
    }

    public InvalidStateTransitionException(String entityName, String fromState, String toState) {
        super("Cannot transition " + entityName + " from state '" + fromState + "' to '" + toState + "'.",
                HttpStatus.BAD_REQUEST, "INVALID_STATE_TRANSITION");
    }
}
