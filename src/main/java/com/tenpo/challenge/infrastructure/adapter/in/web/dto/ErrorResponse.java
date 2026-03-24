package com.tenpo.challenge.infrastructure.adapter.in.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "Standard error response envelope")
public record ErrorResponse(
        @Schema(description = "HTTP status code", example = "503")
        int status,
        @Schema(description = "Short error category", example = "Service Unavailable")
        String error,
        @Schema(description = "Human-readable error detail")
        String message,
        @Schema(description = "Timestamp when the error occurred")
        LocalDateTime timestamp
) {}
