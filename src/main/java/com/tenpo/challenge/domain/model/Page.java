package com.tenpo.challenge.domain.model;

import java.util.List;
import java.util.function.Function;

/**
 * Generic domain pagination wrapper. Kept framework-agnostic.
 */
public record Page<T>(
        List<T> content,
        int pageNumber,
        int pageSize,
        long totalElements,
        int totalPages
) {
    public boolean isFirst() {
        return pageNumber == 0;
    }

    public boolean isLast() {
        return pageNumber >= totalPages - 1;
    }

    public <R> Page<R> map(Function<T, R> mapper) {
        return new Page<>(
                content.stream().map(mapper).toList(),
                pageNumber,
                pageSize,
                totalElements,
                totalPages
        );
    }
}
