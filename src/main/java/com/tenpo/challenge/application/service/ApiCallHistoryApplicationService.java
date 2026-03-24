package com.tenpo.challenge.application.service;

import com.tenpo.challenge.domain.model.ApiCall;
import com.tenpo.challenge.domain.model.Page;
import com.tenpo.challenge.domain.port.in.ApiCallHistoryUseCase;
import com.tenpo.challenge.domain.port.out.ApiCallRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class ApiCallHistoryApplicationService implements ApiCallHistoryUseCase {

    private final ApiCallRepositoryPort repositoryPort;

    @Override
    public Mono<Void> record(ApiCall apiCall) {
        return repositoryPort.save(apiCall).then();
    }

    @Override
    public Mono<Page<ApiCall>> getHistory(int page, int size) {
        return repositoryPort.findAll(page, size);
    }
}
