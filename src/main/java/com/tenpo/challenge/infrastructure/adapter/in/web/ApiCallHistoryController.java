package com.tenpo.challenge.infrastructure.adapter.in.web;

import com.tenpo.challenge.domain.port.in.ApiCallHistoryUseCase;
import com.tenpo.challenge.infrastructure.adapter.in.web.dto.ApiCallResponse;
import com.tenpo.challenge.infrastructure.adapter.in.web.dto.PagedResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "History", description = "Retrieves the paginated log of all API calls")
public class ApiCallHistoryController {

    private final ApiCallHistoryUseCase historyUseCase;

    @GetMapping("/history")
    @Operation(summary = "Get API call history (paginated)",
            description = "Returns all recorded API calls sorted by timestamp descending. " +
                    "This endpoint is excluded from the call log to prevent infinite recursion.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "History retrieved",
                    content = @Content(schema = @Schema(implementation = PagedResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid pagination parameters",
                    content = @Content(schema = @Schema(implementation = PagedResponse.class)))
    })
    public Mono<PagedResponse<ApiCallResponse>> getHistory(
            @Parameter(description = "Zero-based page index", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size (1–100)", example = "10")
            @RequestParam(defaultValue = "10") int size) {

        return historyUseCase.getHistory(page, size)
                .map(domainPage -> PagedResponse.from(domainPage, ApiCallResponse::from));
    }
}
