package com.jircik.calorietrackerapi.domain.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record UpdateMealRequest(

        @Schema(description = "New date and time of the meal (ISO 8601)", example = "2026-04-08T13:00:00")
        @NotNull(message = "DateTime is required")
        LocalDateTime dateTime

) {}
