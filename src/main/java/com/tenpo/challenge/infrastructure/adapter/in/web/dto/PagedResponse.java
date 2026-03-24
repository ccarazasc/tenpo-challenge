package com.tenpo.challenge.infrastructure.adapter.in.web.dto;

import com.tenpo.challenge.domain.model.Page;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.function.Function;

@Schema(description = "Paginated response wrapper")
public record PagedResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last
) {
    public static <S, T> PagedResponse<T> from(Page<S> domainPage, Function<S, T> mapper) {
        return new PagedResponse<>(
                domainPage.content().stream().map(mapper).toList(),
                domainPage.pageNumber(),
                domainPage.pageSize(),
                domainPage.totalElements(),
                domainPage.totalPages(),
                domainPage.isFirst(),
                domainPage.isLast()
        );
    }
}
