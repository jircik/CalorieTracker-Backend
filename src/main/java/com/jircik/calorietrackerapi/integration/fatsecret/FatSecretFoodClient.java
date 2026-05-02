package com.jircik.calorietrackerapi.integration.fatsecret;

import com.jircik.calorietrackerapi.exception.IntegrationException;
import com.jircik.calorietrackerapi.integration.dto.FoodDetailsResponse;
import com.jircik.calorietrackerapi.integration.dto.FoodSearchResponse;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import com.github.benmanes.caffeine.cache.Cache;

import java.util.List;

@Service
public class FatSecretFoodClient {

    private final WebClient webClient;
    private final Cache<String, FoodDetailsResponse> foodDetailsCache;
    private final Cache<String, List<FoodSearchResponse.Food>> foodSearchCache;

    public FatSecretFoodClient(WebClient fatSecretApiWebClient,
                               Cache<String, FoodDetailsResponse> foodDetailsCache,
                               Cache<String, List<FoodSearchResponse.Food>> foodSearchCache) {
        this.webClient = fatSecretApiWebClient;
        this.foodDetailsCache = foodDetailsCache;
        this.foodSearchCache = foodSearchCache;
    }

    private String normalizeFoodName(String foodName) {
        return foodName.trim().toLowerCase().replaceAll("\\s+", " ");
    }

    public List<FoodSearchResponse.Food> searchFoods(String query) {
        String normalized = normalizeFoodName(query);
        List<FoodSearchResponse.Food> cached = foodSearchCache.getIfPresent(normalized);
        if (cached != null) return cached;

        FoodSearchResponse response = webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/search")
                        .queryParam("q", query)
                        .build())
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, clientResponse ->
                        clientResponse.bodyToMono(String.class)
                                .map(body -> new IntegrationException("FatSecret client error: " + body))
                )
                .onStatus(HttpStatusCode::is5xxServerError, clientResponse ->
                        clientResponse.bodyToMono(String.class)
                                .map(body -> new IntegrationException("FatSecret server error: " + body))
                )
                .bodyToMono(FoodSearchResponse.class)
                .block();

        if (response == null
                || response.foods() == null
                || response.foods().food() == null
                || response.foods().food().isEmpty()) {
            throw new IntegrationException("No results found for: " + query);
        }

        List<FoodSearchResponse.Food> results = response.foods().food().stream().limit(5).toList();
        foodSearchCache.put(normalized, results);
        return results;
    }

    public FoodDetailsResponse getFoodById(String foodId) {
        FoodDetailsResponse cached = foodDetailsCache.getIfPresent(foodId);
        if (cached != null) return cached;

        FoodDetailsResponse response = webClient.get()
                .uri("/food/{id}", foodId)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, clientResponse ->
                        clientResponse.bodyToMono(String.class)
                                .map(body -> new IntegrationException(
                                        "FatSecret client error (getFoodById): " + body))
                )
                .onStatus(HttpStatusCode::is5xxServerError, clientResponse ->
                        clientResponse.bodyToMono(String.class)
                                .map(body -> new IntegrationException(
                                        "FatSecret server error (getFoodById): " + body))
                )
                .bodyToMono(FoodDetailsResponse.class)
                .block();

        if (response == null || response.food() == null) {
            throw new IntegrationException("Invalid response from FatSecret (getFoodById)");
        }

        foodDetailsCache.put(foodId, response);
        return response;
    }
}
