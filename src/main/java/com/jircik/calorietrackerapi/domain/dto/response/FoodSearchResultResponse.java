package com.jircik.calorietrackerapi.domain.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record FoodSearchResultResponse(
        @Schema(description = "FatSecret food ID — pass this to addFoodToMeal") String foodId,
        @Schema(description = "Food name") String foodName,
        @Schema(description = "Brand name, null for generic foods") String brandName,
        @Schema(description = "Nutritional summary description from FatSecret") String description
) {}