package com.tenpo.challenge.infrastructure.exception;

/**
 * Thrown when a client exceeds the allowed requests-per-minute threshold.
 */
public class RateLimitExceededException extends RuntimeException {

    public RateLimitExceededException(String message) {
        super(message);
    }
}
