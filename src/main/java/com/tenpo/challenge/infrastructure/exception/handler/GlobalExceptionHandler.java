package com.tenpo.challenge.infrastructure.exception.handler;

import com.tenpo.challenge.infrastructure.adapter.in.web.dto.ErrorResponse;
import com.tenpo.challenge.infrastructure.exception.PercentageServiceUnavailableException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

/**
 * Centralised HTTP error handler for the 4XX/5XX series.
 *
 * Each handler returns a consistent {@link ErrorResponse} JSON envelope so that
 * API consumers always receive the same error shape regardless of the failure origin.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(PercentageServiceUnavailableException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public Mono<ErrorResponse> handlePercentageUnavailable(PercentageServiceUnavailableException ex) {
        log.warn("Percentage service unavailable: {}", ex.getMessage());
        return Mono.just(error(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage()));
    }

    @ExceptionHandler(WebExchangeBindException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Mono<ErrorResponse> handleValidation(WebExchangeBindException ex) {
        String detail = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> "'%s' %s".formatted(fe.getField(), fe.getDefaultMessage()))
                .collect(Collectors.joining("; "));
        return Mono.just(error(HttpStatus.BAD_REQUEST, detail.isBlank() ? ex.getMessage() : detail));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleResponseStatus(ResponseStatusException ex) {
        HttpStatus status = HttpStatus.resolve(ex.getStatusCode().value());
        if (status == null) status = HttpStatus.INTERNAL_SERVER_ERROR;
        log.debug("ResponseStatusException: {}", ex.getMessage());
        String message = ex.getReason() != null ? ex.getReason() : ex.getMessage();
        return Mono.just(ResponseEntity.status(status).body(error(status, message)));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Mono<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        return Mono.just(error(HttpStatus.BAD_REQUEST, ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Mono<ErrorResponse> handleGeneric(Exception ex) {
        log.error("Unhandled exception: {}", ex.getMessage(), ex);
        return Mono.just(error(HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred. Please try again later."));
    }

    private ErrorResponse error(HttpStatus status, String message) {
        return new ErrorResponse(status.value(), status.getReasonPhrase(), message, LocalDateTime.now());
    }
}
