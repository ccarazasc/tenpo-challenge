package com.tenpo.challenge.infrastructure.adapter.out.persistence;

import com.tenpo.challenge.domain.model.ApiCall;
import com.tenpo.challenge.domain.model.Page;
import com.tenpo.challenge.domain.port.out.ApiCallRepositoryPort;
import com.tenpo.challenge.infrastructure.adapter.out.persistence.entity.ApiCallEntity;
import com.tenpo.challenge.infrastructure.adapter.out.persistence.repository.R2dbcApiCallRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ApiCallRepositoryAdapter implements ApiCallRepositoryPort {

    private final R2dbcApiCallRepository repository;

    @Override
    public Mono<ApiCall> save(ApiCall apiCall) {
        return repository.save(toEntity(apiCall)).map(this::toDomain);
    }

    @Override
    public Mono<Page<ApiCall>> findAll(int page, int size) {
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "timestamp"));

        return Mono.zip(
                repository.findAllBy(pageable).collectList(),
                repository.count()
        ).map(tuple -> {
            List<ApiCall> content = tuple.getT1().stream().map(this::toDomain).toList();
            long total = tuple.getT2();
            int totalPages = (int) Math.ceil((double) total / size);
            return new Page<>(content, page, size, total, totalPages);
        });
    }

    private ApiCallEntity toEntity(ApiCall domain) {
        return ApiCallEntity.builder()
                .endpoint(domain.endpoint())
                .parameters(domain.parameters())
                .response(domain.response())
                .success(domain.success())
                .timestamp(domain.timestamp())
                .build();
    }

    private ApiCall toDomain(ApiCallEntity entity) {
        return new ApiCall(
                entity.getId(),
                entity.getEndpoint(),
                entity.getParameters(),
                entity.getResponse(),
                entity.isSuccess(),
                entity.getTimestamp()
        );
    }
}
