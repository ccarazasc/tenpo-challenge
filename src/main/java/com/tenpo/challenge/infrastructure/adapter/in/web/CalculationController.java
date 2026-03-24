package com.tenpo.challenge.infrastructure.adapter.in.web;

import com.tenpo.challenge.domain.port.in.CalculationUseCase;
import com.tenpo.challenge.infrastructure.adapter.in.web.dto.CalculationResponse;
import com.tenpo.challenge.infrastructure.adapter.in.web.dto.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@Validated
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Calculation", description = "Calculates the sum of two numbers with a dynamic external percentage")
public class CalculationController {

    private final CalculationUseCase calculationUseCase;

    @GetMapping("/calculate")
    @Operation(summary = "Calculate (num1 + num2) + percentage%",
            description = "Fetches the percentage from an external service (cached 30 min). " +
                    "Falls back to the last cached value if the service is unavailable. " +
                    "Returns 503 if no cached value exists.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Calculation succeeded",
                    content = @Content(schema = @Schema(implementation = CalculationResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input parameters",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "429", description = "Rate limit exceeded",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "503", description = "Percentage service unavailable and no cache",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public Mono<CalculationResponse> calculate(
            @Parameter(description = "First number", required = true, example = "5")
            @RequestParam @NotNull Double num1,
            @Parameter(description = "Second number", required = true, example = "5")
            @RequestParam @NotNull Double num2) {

        return calculationUseCase.calculate(num1, num2)
                .map(result -> new CalculationResponse(num1, num2, result));
    }
}
