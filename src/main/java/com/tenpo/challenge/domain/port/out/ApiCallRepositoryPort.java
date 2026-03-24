package com.tenpo.challenge.domain.port.out;

import com.tenpo.challenge.domain.model.ApiCall;
import com.tenpo.challenge.domain.model.Page;
import reactor.core.publisher.Mono;

/**
 * Output port: persistence contract for API call history.
 */
public interface ApiCallRepositoryPort {
    Mono<ApiCall> save(ApiCall apiCall);
    Mono<Page<ApiCall>> findAll(int page, int size);
}
