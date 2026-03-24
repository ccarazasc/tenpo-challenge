package com.tenpo.challenge.domain.port.in;

import com.tenpo.challenge.domain.model.ApiCall;
import com.tenpo.challenge.domain.model.Page;
import reactor.core.publisher.Mono;

/**
 * Input port: records and retrieves the history of API calls.
 */
public interface ApiCallHistoryUseCase {
    Mono<Void> record(ApiCall apiCall);
    Mono<Page<ApiCall>> getHistory(int page, int size);
}
