package com.tenpo.challenge.infrastructure.adapter.in.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Result of the calculation endpoint")
public record CalculationResponse(
        @Schema(description = "First input number", example = "5.0")
        double num1,
        @Schema(description = "Second input number", example = "5.0")
        double num2,
        @Schema(description = "Result: (num1 + num2) + percentage%", example = "11.0")
        double result
) {}
