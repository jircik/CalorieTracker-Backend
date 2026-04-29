package com.jircik.calorietrackerapi.domain.dto.request;

import com.jircik.calorietrackerapi.domain.entity.MealTypeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record CreateMealRequest(

        @Schema(description = "Date and time of the meal (ISO 8601)", example = "2026-04-08T12:30:00")
        @NotNull(message = "DateTime is required")
        LocalDateTime dateTime,

        @Schema(description = "Type of meal")
        @NotNull(message = "Mealtype is required")
        MealTypeEnum mealType

) {}
