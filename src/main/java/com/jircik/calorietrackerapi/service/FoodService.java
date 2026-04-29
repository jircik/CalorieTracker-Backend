package com.jircik.calorietrackerapi.service;

import com.jircik.calorietrackerapi.domain.dto.response.FoodSearchResultResponse;
import com.jircik.calorietrackerapi.integration.fatsecret.FatSecretFoodClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FoodService {

    private final FatSecretFoodClient foodClient;

    public List<FoodSearchResultResponse> searchFoods(String query) {
        if (query == null || query.isBlank()) {
            throw new IllegalArgumentException("Search query must not be blank");
        }
        return foodClient.searchFoods(query).stream()
                .map(f -> new FoodSearchResultResponse(
                        f.food_id(),
                        f.food_name(),
                        f.brand_name(),
                        f.food_description()))
                .toList();
    }
}