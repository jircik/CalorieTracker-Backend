package com.jircik.calorietrackerapi.domain.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record LogWaterRequest(
        @Schema(description = "Amount of water consumed in milliliters", example = "300")
        @NotNull
        @Min(1)
        Integer amountMl) {
}
