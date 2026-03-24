package com.tenpo.challenge.domain.model;

import java.time.LocalDateTime;

/**
 * Domain model representing a recorded API call event.
 * Immutable by design - use the factory method for creation.
 */
public record ApiCall(
        Long id,
        String endpoint,
        String parameters,
        String response,
        boolean success,
        LocalDateTime timestamp
) {
    public static ApiCall of(String endpoint, String parameters, String response, boolean success) {
        return new ApiCall(null, endpoint, parameters, response, success, LocalDateTime.now());
    }
}
