package com.tenpo.challenge.infrastructure.exception;

/**
 * Wraps failures from remote HTTP calls to external services.
 */
public class ExternalServiceException extends RuntimeException {

    public ExternalServiceException(String message) {
        super(message);
    }

    public ExternalServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
