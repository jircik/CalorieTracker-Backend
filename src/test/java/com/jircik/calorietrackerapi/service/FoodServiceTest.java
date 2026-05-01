package com.jircik.calorietrackerapi.service;

import com.jircik.calorietrackerapi.domain.dto.response.FoodSearchResultResponse;
import com.jircik.calorietrackerapi.integration.dto.FoodSearchResponse;
import com.jircik.calorietrackerapi.integration.fatsecret.FatSecretFoodClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FoodServiceTest {

    @Mock
    private FatSecretFoodClient foodClient;

    @InjectMocks
    private FoodService foodService;

    @Test
    @DisplayName("searchFoods — deve mapear resultados do FatSecret para FoodSearchResultResponse")
    void searchFoods_shouldMapResults() {
        FoodSearchResponse.Food f1 = new FoodSearchResponse.Food(
                "123", "Arroz Branco", "Tio João", "Per 100g - 130 kcal", "Generic", null);
        FoodSearchResponse.Food f2 = new FoodSearchResponse.Food(
                "456", "Arroz Integral", null, "Per 100g - 111 kcal", "Generic", null);
        when(foodClient.searchFoods("arroz")).thenReturn(List.of(f1, f2));

        List<FoodSearchResultResponse> result = foodService.searchFoods("arroz");

        assertThat(result).hasSize(2);
        assertThat(result.get(0).foodId()).isEqualTo("123");
        assertThat(result.get(0).foodName()).isEqualTo("Arroz Branco");
        assertThat(result.get(0).brandName()).isEqualTo("Tio João");
        assertThat(result.get(1).brandName()).isNull();
        verify(foodClient).searchFoods("arroz");
    }

    @Test
    @DisplayName("searchFoods — deve retornar lista vazia quando FatSecret retorna vazio")
    void searchFoods_shouldReturnEmpty() {
        when(foodClient.searchFoods("xyz")).thenReturn(List.of());

        List<FoodSearchResultResponse> result = foodService.searchFoods("xyz");

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("searchFoods — deve lançar IllegalArgumentException quando query é null")
    void searchFoods_shouldThrowWhenQueryNull() {
        assertThatThrownBy(() -> foodService.searchFoods(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Search query must not be blank");

        verifyNoInteractions(foodClient);
    }

    @Test
    @DisplayName("searchFoods — deve lançar IllegalArgumentException quando query é em branco")
    void searchFoods_shouldThrowWhenQueryBlank() {
        assertThatThrownBy(() -> foodService.searchFoods("   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Search query must not be blank");

        verifyNoInteractions(foodClient);
    }
}
