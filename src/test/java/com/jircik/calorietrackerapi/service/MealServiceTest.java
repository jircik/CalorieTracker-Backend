package com.jircik.calorietrackerapi.service;

import com.jircik.calorietrackerapi.domain.dto.request.AddFoodToMealRequest;
import com.jircik.calorietrackerapi.domain.dto.response.MealFoodResponse;
import com.jircik.calorietrackerapi.domain.dto.response.MealResponse;
import com.jircik.calorietrackerapi.domain.dto.response.MealSummaryResponse;
import com.jircik.calorietrackerapi.domain.dto.response.MealWithFoodsResponse;
import com.jircik.calorietrackerapi.domain.entity.Meal;
import com.jircik.calorietrackerapi.domain.entity.MealFood;
import com.jircik.calorietrackerapi.domain.entity.MealTypeEnum;
import com.jircik.calorietrackerapi.domain.entity.User;
import com.jircik.calorietrackerapi.domain.fatsecret.NutritionProvider;
import com.jircik.calorietrackerapi.domain.fatsecret.NutritionResult;
import com.jircik.calorietrackerapi.exception.ResourceNotFoundException;
import com.jircik.calorietrackerapi.repository.MealFoodRepository;
import com.jircik.calorietrackerapi.repository.MealRepository;
import com.jircik.calorietrackerapi.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MealServiceTest {

    @Mock
    private MealRepository mealRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private MealFoodRepository mealFoodRepository;

    @Mock
    private NutritionProvider nutritionProvider;

    @InjectMocks
    private MealService mealService;

    private User testUser;
    private Meal testMeal;

    @BeforeEach
    void setUp() {
        testUser = User.builder().id(1L).name("João").email("joao@email.com").build();

        testMeal = new Meal();
        testMeal.setId(10L);
        testMeal.setUser(testUser);
        testMeal.setDatetime(LocalDateTime.of(2026, 3, 10, 12, 0));
        testMeal.setCreatedAt(LocalDateTime.now());
    }

    // createMeal
    @Nested
    @DisplayName("createMeal")
    class CreateMeal {

        @Test
        @DisplayName("deve criar uma refeição com sucesso")
        void shouldCreateMealSuccessfully() {
            LocalDateTime dateTime = LocalDateTime.of(2026, 3, 10, 12, 0);
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(mealRepository.save(any(Meal.class))).thenReturn(testMeal);

            MealResponse response = mealService.createMeal(1L, dateTime, MealTypeEnum.BREAKFAST);

            assertThat(response.id()).isEqualTo(10L);
            assertThat(response.userId()).isEqualTo(1L);
            assertThat(response.dateTime()).isEqualTo(dateTime);
            verify(mealRepository).save(any(Meal.class));
        }

        @Test
        @DisplayName("deve lançar exceção quando usuário não existir")
        void shouldThrowWhenUserNotFound() {
            when(userRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> mealService.createMeal(99L, LocalDateTime.now(), MealTypeEnum.BREAKFAST))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("User not found");
        }
    }

    // addFoodToMeal
    @Nested
    @DisplayName("addFoodToMeal")
    class AddFoodToMeal {

        private final AddFoodToMealRequest request =
                new AddFoodToMealRequest("123", "Arroz", 150.0, "g");

        @Test
        @DisplayName("deve adicionar alimento com sucesso usando foodId")
        void shouldAddFoodByFoodId() {
            NutritionResult nutrition = new NutritionResult("123", "Arroz", 195.0, 43.0, 4.0, 0.4);
            MealFood savedFood = MealFood.builder()
                    .id(1L).meal(testMeal).foodName("Arroz").fatSecretFoodId("123")
                    .quantity(150.0).unit("g").calories(195.0).carbs(43.0).protein(4.0).fat(0.4).build();

            when(mealRepository.findById(10L)).thenReturn(Optional.of(testMeal));
            when(nutritionProvider.calculateNutritionByFoodId("123", 150.0)).thenReturn(nutrition);
            when(mealFoodRepository.save(any(MealFood.class))).thenReturn(savedFood);

            MealFoodResponse response = mealService.addFoodToMeal(10L, request, 1L);

            assertThat(response.foodName()).isEqualTo("Arroz");
            assertThat(response.calories()).isEqualTo(195.0);
            verify(nutritionProvider).calculateNutritionByFoodId("123", 150.0);
        }

        @Test
        @DisplayName("deve lançar exceção quando refeição não existir")
        void shouldThrowWhenMealNotFound() {
            when(mealRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> mealService.addFoodToMeal(99L, request, 1L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Meal not found");
        }

        @Test
        @DisplayName("deve lançar 403 quando caller não é o dono da refeição")
        void shouldThrowForbiddenWhenNotOwner() {
            when(mealRepository.findById(10L)).thenReturn(Optional.of(testMeal));

            assertThatThrownBy(() -> mealService.addFoodToMeal(10L, request, 2L))
                    .isInstanceOf(org.springframework.web.server.ResponseStatusException.class)
                    .satisfies(e -> assertThat(
                            ((org.springframework.web.server.ResponseStatusException) e).getStatusCode())
                            .isEqualTo(org.springframework.http.HttpStatus.FORBIDDEN));
        }
    }

    // DeleteMeal
    @Nested
    @DisplayName("DeleteMeal")
    class DeleteMeal {

        @Test
        @DisplayName("deve deletar refeição quando ela existe")
        void shouldDeleteMealWhenExists() {
            when(mealRepository.findById(10L)).thenReturn(Optional.of(testMeal));

            mealService.DeleteMeal(10L, 1L);

            verify(mealRepository).deleteById(10L);
        }

        @Test
        @DisplayName("deve lançar exceção quando refeição não existe")
        void shouldThrowWhenMealNotFound() {
            when(mealRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> mealService.DeleteMeal(99L, 1L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Meal not found!");
        }
    }

    // DeleteMealFood
    @Nested
    @DisplayName("DeleteMealFood")
    class DeleteMealFood {

        @Test
        @DisplayName("deve deletar alimento quando encontrado")
        void shouldDeleteMealFoodWhenFound() {
            MealFood mealFood = MealFood.builder().id(1L).meal(testMeal).build();
            when(mealRepository.findById(10L)).thenReturn(Optional.of(testMeal));
            when(mealFoodRepository.findByIdAndMeal_Id(1L, 10L)).thenReturn(Optional.of(mealFood));

            mealService.DeleteMealFood(10L, 1L, 1L);

            verify(mealFoodRepository).delete(mealFood);
        }

        @Test
        @DisplayName("deve lançar exceção quando alimento não existe")
        void shouldThrowWhenMealFoodNotFound() {
            when(mealRepository.findById(10L)).thenReturn(Optional.of(testMeal));
            when(mealFoodRepository.findByIdAndMeal_Id(99L, 10L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> mealService.DeleteMealFood(10L, 99L, 1L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("MealFood not found!");
        }
    }

    // updateMealFoodQuantity
    @Nested
    @DisplayName("updateMealFoodQuantity")
    class UpdateMealFoodQuantity {

        @Test
        @DisplayName("deve recalcular nutrição com nova quantidade")
        void shouldRecalculateNutritionForNewQuantity() {
            MealFood existing = MealFood.builder()
                    .id(1L).meal(testMeal).foodName("Arroz").fatSecretFoodId("123")
                    .quantity(150.0).unit("g").calories(195.0).carbs(43.0).protein(4.0).fat(0.4)
                    .build();

            NutritionResult nutrition = new NutritionResult("123", "Arroz", 260.0, 57.3, 5.3, 0.5);

            when(mealRepository.findById(10L)).thenReturn(Optional.of(testMeal));
            when(mealFoodRepository.findByIdAndMeal_Id(1L, 10L)).thenReturn(Optional.of(existing));
            when(nutritionProvider.calculateNutritionByFoodId("123", 200.0)).thenReturn(nutrition);

            MealFoodResponse response = mealService.updateMealFoodQuantity(10L, 1L, 200.0, 1L);

            assertThat(response.quantity()).isEqualTo(200.0);
            assertThat(response.calories()).isEqualTo(260.0);
            assertThat(response.protein()).isEqualTo(5.3);
            verify(mealFoodRepository).save(existing);
        }

        @Test
        @DisplayName("deve lançar exceção quando alimento não existe")
        void shouldThrowWhenMealFoodNotFound() {
            when(mealRepository.findById(10L)).thenReturn(Optional.of(testMeal));
            when(mealFoodRepository.findByIdAndMeal_Id(99L, 10L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> mealService.updateMealFoodQuantity(10L, 99L, 200.0, 1L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("MealFood not found!");
        }

        @Test
        @DisplayName("deve lançar exceção quando quantidade é zero")
        void shouldThrowWhenQuantityIsZero() {
            when(mealRepository.findById(10L)).thenReturn(Optional.of(testMeal));
            assertThatThrownBy(() -> mealService.updateMealFoodQuantity(10L, 1L, 0.0, 1L))
                    .isInstanceOf(RuntimeException.class);
        }

        @Test
        @DisplayName("deve lançar exceção quando quantidade é negativa")
        void shouldThrowWhenQuantityIsNegative() {
            when(mealRepository.findById(10L)).thenReturn(Optional.of(testMeal));
            assertThatThrownBy(() -> mealService.updateMealFoodQuantity(10L, 1L, -10.0, 1L))
                    .isInstanceOf(RuntimeException.class);
        }
    }

    // getMealSummary
    @Nested
    @DisplayName("getMealSummary")
    class GetMealSummary {

        @Test
        @DisplayName("deve calcular soma dos macros corretamente")
        void shouldCalculateMacroTotals() {
            MealFood food1 = MealFood.builder()
                    .calories(200.0).protein(10.0).carbs(30.0).fat(5.0).build();
            MealFood food2 = MealFood.builder()
                    .calories(350.0).protein(25.0).carbs(40.0).fat(12.0).build();

            when(mealRepository.findById(10L)).thenReturn(Optional.of(testMeal));
            when(mealFoodRepository.findByMeal_Id(10L)).thenReturn(List.of(food1, food2));

            MealSummaryResponse response = mealService.getMealSummary(10L, 1L);

            assertThat(response.mealId()).isEqualTo(10L);
            assertThat(response.totalCalories()).isEqualTo(550.0);
            assertThat(response.totalProtein()).isEqualTo(35.0);
            assertThat(response.totalCarbs()).isEqualTo(70.0);
            assertThat(response.totalFat()).isEqualTo(17.0);
            assertThat(response.foodCount()).isEqualTo(2.0);
        }

        @Test
        @DisplayName("deve retornar zeros quando não há alimentos")
        void shouldReturnZerosWhenNoFoods() {
            when(mealRepository.findById(10L)).thenReturn(Optional.of(testMeal));
            when(mealFoodRepository.findByMeal_Id(10L)).thenReturn(Collections.emptyList());

            MealSummaryResponse response = mealService.getMealSummary(10L, 1L);

            assertThat(response.totalCalories()).isEqualTo(0.0);
            assertThat(response.foodCount()).isEqualTo(0.0);
        }

        @Test
        @DisplayName("deve lançar exceção quando refeição não existir")
        void shouldThrowWhenMealNotFound() {
            when(mealRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> mealService.getMealSummary(99L, 1L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Meal not found!");
        }

        @Test
        @DisplayName("deve lançar 403 quando caller não é o dono da refeição")
        void shouldThrowForbiddenWhenNotOwner() {
            when(mealRepository.findById(10L)).thenReturn(Optional.of(testMeal));
            // testMeal belongs to userId=1L; pass callerUserId=2L
            assertThatThrownBy(() -> mealService.getMealSummary(10L, 2L))
                    .isInstanceOf(org.springframework.web.server.ResponseStatusException.class)
                    .satisfies(e -> {
                        org.springframework.web.server.ResponseStatusException rse =
                                (org.springframework.web.server.ResponseStatusException) e;
                        assertThat(rse.getStatusCode()).isEqualTo(org.springframework.http.HttpStatus.FORBIDDEN);
                    });
        }
    }

    // getMealWithFoods
    @Nested
    @DisplayName("getMealWithFoods")
    class GetMealWithFoods {

        @Test
        @DisplayName("deve retornar refeição com alimentos e totais calculados")
        void shouldReturnMealWithFoodsAndTotals() {
            MealFood food1 = MealFood.builder()
                    .id(1L).meal(testMeal).foodName("Arroz").fatSecretFoodId("123")
                    .quantity(150.0).unit("g").calories(195.0).carbs(43.0).protein(4.0).fat(0.4).build();
            MealFood food2 = MealFood.builder()
                    .id(2L).meal(testMeal).foodName("Feijão").fatSecretFoodId("456")
                    .quantity(100.0).unit("g").calories(77.0).carbs(14.0).protein(5.0).fat(0.5).build();

            when(mealRepository.findById(10L)).thenReturn(Optional.of(testMeal));
            when(mealFoodRepository.findByMeal_Id(10L)).thenReturn(List.of(food1, food2));

            MealWithFoodsResponse response = mealService.getMealWithFoods(10L, 1L);

            assertThat(response.mealId()).isEqualTo(10L);
            assertThat(response.foods()).hasSize(2);
            assertThat(response.totalCalories()).isEqualTo(272.0);
            assertThat(response.totalProtein()).isEqualTo(9.0);
            assertThat(response.totalCarbs()).isEqualTo(57.0);
            assertThat(response.totalFat()).isEqualTo(0.9);
        }

        @Test
        @DisplayName("deve retornar refeição vazia sem alimentos")
        void shouldReturnMealWithNoFoods() {
            when(mealRepository.findById(10L)).thenReturn(Optional.of(testMeal));
            when(mealFoodRepository.findByMeal_Id(10L)).thenReturn(Collections.emptyList());

            MealWithFoodsResponse response = mealService.getMealWithFoods(10L, 1L);

            assertThat(response.foods()).isEmpty();
            assertThat(response.totalCalories()).isEqualTo(0.0);
        }

        @Test
        @DisplayName("deve lançar exceção quando refeição não existir")
        void shouldThrowWhenMealNotFound() {
            when(mealRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> mealService.getMealWithFoods(99L, 1L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Meal not found");
        }

        @Test
        @DisplayName("deve lançar 403 quando caller não é o dono")
        void shouldThrowForbiddenWhenNotOwner() {
            when(mealRepository.findById(10L)).thenReturn(Optional.of(testMeal));

            assertThatThrownBy(() -> mealService.getMealWithFoods(10L, 2L))
                    .isInstanceOf(org.springframework.web.server.ResponseStatusException.class)
                    .satisfies(e -> assertThat(
                            ((org.springframework.web.server.ResponseStatusException) e).getStatusCode())
                            .isEqualTo(org.springframework.http.HttpStatus.FORBIDDEN));
        }
    }
}