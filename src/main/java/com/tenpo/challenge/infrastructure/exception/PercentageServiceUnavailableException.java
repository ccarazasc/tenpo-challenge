package com.tenpo.challenge.infrastructure.exception;

/**
 * Thrown when the external percentage service is unavailable and no cached value exists.
 */
public class PercentageServiceUnavailableException extends RuntimeException {

    public PercentageServiceUnavailableException(String message) {
        super(message);
    }

    public PercentageServiceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
