package com.tenpo.challenge.infrastructure.adapter.in.web.dto;

import com.tenpo.challenge.domain.model.ApiCall;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "Recorded API call entry in the history log")
public record ApiCallResponse(
        @Schema(description = "Auto-generated identifier")
        Long id,
        @Schema(description = "Invoked endpoint path", example = "/api/v1/calculate")
        String endpoint,
        @Schema(description = "Query parameters at call time", example = "{num1=[5.0], num2=[5.0]}")
        String parameters,
        @Schema(description = "Response body or error message returned to the caller")
        String response,
        @Schema(description = "Whether the call completed successfully")
        boolean success,
        @Schema(description = "UTC timestamp of the call")
        LocalDateTime timestamp
) {
    public static ApiCallResponse from(ApiCall apiCall) {
        return new ApiCallResponse(
                apiCall.id(),
                apiCall.endpoint(),
                apiCall.parameters(),
                apiCall.response(),
                apiCall.success(),
                apiCall.timestamp()
        );
    }
}
