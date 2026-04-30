package com.jircik.calorietrackerapi.domain.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record MealSummaryResponse(

        @Schema(description = "ID of the meal")
        Long mealId,

        @Schema(description = "Total calories across all foods in this meal")
        Double totalCalories,

        @Schema(description = "Total protein in grams")
        Double totalProtein,

        @Schema(description = "Total carbohydrates in grams")
        Double totalCarbs,

        @Schema(description = "Total fat in grams")
        Double totalFat,

        @Schema(description = "Number of food entries in this meal")
        Integer foodCount

) {}