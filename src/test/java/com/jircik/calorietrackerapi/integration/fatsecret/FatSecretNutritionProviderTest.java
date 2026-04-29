package com.jircik.calorietrackerapi.integration.fatsecret;

import com.jircik.calorietrackerapi.domain.entity.FoodNutrition;
import com.jircik.calorietrackerapi.domain.fatsecret.NutritionResult;
import com.jircik.calorietrackerapi.integration.dto.FoodDetailsResponse;
import com.jircik.calorietrackerapi.repository.FoodNutritionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FatSecretNutritionProviderTest {

    @Mock
    private FatSecretFoodClient foodClient;

    @Mock
    private FoodNutritionRepository foodNutritionRepository;

    @InjectMocks
    private FatSecretNutritionProvider provider;

    // calculateNutritionByFoodId
    @Nested
    @DisplayName("calculateNutritionByFoodId")
    class CalculateNutritionByFoodId {

        @Test
        @DisplayName("deve usar cache do DB quando FoodNutrition existe")
        void shouldUseCacheWhenNutritionExists() {
            FoodNutrition cached = FoodNutrition.builder()
                    .fatSecretFoodId("123")
                    .caloriesPer100g(130.0)
                    .carbsPer100g(28.0)
                    .proteinPer100g(2.7)
                    .fatPer100g(0.3)
                    .build();

            when(foodNutritionRepository.findById("123")).thenReturn(Optional.of(cached));

            NutritionResult result = provider.calculateNutritionByFoodId("123", 150.0);

            // 130 * 1.5 = 195.0
            assertThat(result.calories()).isEqualTo(195.0);
            // 28 * 1.5 = 42.0
            assertThat(result.carbs()).isEqualTo(42.0);
            assertThat(result.foodId()).isEqualTo("123");
            verify(foodClient, never()).getFoodById(any());
        }

        @Test
        @DisplayName("deve buscar na API quando FoodNutrition não existe no DB")
        void shouldCallApiWhenNotCached() {
            when(foodNutritionRepository.findById("123")).thenReturn(Optional.empty());

            FoodDetailsResponse.Serving serving = new FoodDetailsResponse.Serving(
                    "1", "Per 100g", "100.00", "g",
                    "130.00", "28.00", "2.70", "0.30", "1");
            FoodDetailsResponse.Servings servings = new FoodDetailsResponse.Servings(List.of(serving));
            FoodDetailsResponse.Food food = new FoodDetailsResponse.Food("123", "Arroz", null, servings);
            FoodDetailsResponse details = new FoodDetailsResponse(food);

            when(foodClient.getFoodById("123")).thenReturn(details);

            NutritionResult result = provider.calculateNutritionByFoodId("123", 200.0);

            // 130 * (200/100) = 260.0
            assertThat(result.calories()).isEqualTo(260.0);
            verify(foodNutritionRepository).save(any(FoodNutrition.class));
        }

        @Test
        @DisplayName("deve lançar exceção quando foodId é nulo")
        void shouldThrowWhenFoodIdNull() {
            assertThatThrownBy(() -> provider.calculateNutritionByFoodId(null, 100.0))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Food id must not be null");
        }

        @Test
        @DisplayName("deve lançar exceção quando foodId é vazio")
        void shouldThrowWhenFoodIdBlank() {
            assertThatThrownBy(() -> provider.calculateNutritionByFoodId("  ", 100.0))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("deve lançar exceção quando quantity é zero ou negativa")
        void shouldThrowWhenQuantityInvalid() {
            assertThatThrownBy(() -> provider.calculateNutritionByFoodId("123", 0.0))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Quantity must be greater than zero");

            assertThatThrownBy(() -> provider.calculateNutritionByFoodId("123", -5.0))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }
}
