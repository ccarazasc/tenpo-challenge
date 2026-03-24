package com.tenpo.challenge.infrastructure.adapter.out.persistence.repository;

import com.tenpo.challenge.infrastructure.adapter.out.persistence.entity.ApiCallEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

@Repository
public interface R2dbcApiCallRepository extends ReactiveCrudRepository<ApiCallEntity, Long> {

    /**
     * Returns all records ordered by the specified Pageable (supports sorting + pagination).
     */
    Flux<ApiCallEntity> findAllBy(Pageable pageable);
}
