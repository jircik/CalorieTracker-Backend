package com.jircik.calorietrackerapi.domain.fatsecret;

public interface NutritionProvider {
    NutritionResult calculateNutritionByFoodId(String foodId, Double quantityInGrams);
}