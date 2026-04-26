package com.jircik.calorietrackerapi.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.jircik.calorietrackerapi.domain.dto.request.AddFoodToMealRequest;
import com.jircik.calorietrackerapi.domain.dto.request.CreateMealRequest;
import com.jircik.calorietrackerapi.domain.dto.request.UpdateMealFoodQuantityRequest;
import com.jircik.calorietrackerapi.domain.dto.response.MealFoodResponse;
import com.jircik.calorietrackerapi.domain.dto.response.MealResponse;
import com.jircik.calorietrackerapi.domain.dto.response.MealSummaryResponse;
import com.jircik.calorietrackerapi.domain.entity.MealTypeEnum;
import com.jircik.calorietrackerapi.exception.ResourceNotFoundException;
import com.jircik.calorietrackerapi.security.JwtService;
import org.springframework.security.core.userdetails.UserDetailsService;
import com.jircik.calorietrackerapi.service.MealService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static com.jircik.calorietrackerapi.util.SecurityTestUtils.*;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MealController.class)
public class MealControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    @MockitoBean
    private MealService mealService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    // ── createMeal ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("POST /meals — deve criar refeição e retornar 201")
    void createMeal_shouldReturn201() throws Exception {
        LocalDateTime dateTime = LocalDateTime.of(2026, 3, 10, 12, 0);
        CreateMealRequest request = new CreateMealRequest(1L, dateTime, MealTypeEnum.BREAKFAST);
        MealResponse response = new MealResponse(10L, 1L, dateTime, MealTypeEnum.BREAKFAST, LocalDateTime.now());

        when(mealService.createMeal(eq(1L), eq(dateTime), eq(MealTypeEnum.BREAKFAST), eq(1L)))
                .thenReturn(response);

        mockMvc.perform(post("/meals")
                        .with(authentication(authAs(1L))).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.mealType").value("BREAKFAST"));
    }

    @Test
    @DisplayName("POST /meals — deve retornar 400 quando userId é nulo")
    void createMeal_shouldReturn400WhenUserIdNull() throws Exception {
        CreateMealRequest request = new CreateMealRequest(null, LocalDateTime.now(), MealTypeEnum.LUNCH);

        mockMvc.perform(post("/meals")
                        .with(authentication(authAs(1L))).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /meals — deve retornar 400 quando dateTime é nulo")
    void createMeal_shouldReturn400WhenDateTimeNull() throws Exception {
        CreateMealRequest request = new CreateMealRequest(1L, null, MealTypeEnum.LUNCH);

        mockMvc.perform(post("/meals")
                        .with(authentication(authAs(1L))).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /meals — deve retornar 400 quando mealType é nulo")
    void createMeal_shouldReturn400WhenMealTypeNull() throws Exception {
        CreateMealRequest request = new CreateMealRequest(1L, LocalDateTime.now(), null);

        mockMvc.perform(post("/meals")
                        .with(authentication(authAs(1L))).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // ── addFoodToMeal ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("POST /meals/{mealId}/foods — deve adicionar alimento e retornar 201")
    void addFoodToMeal_shouldReturn201() throws Exception {
        AddFoodToMealRequest request = new AddFoodToMealRequest("Arroz", 150.0, "g");
        MealFoodResponse response = new MealFoodResponse(
                1L, "Arroz", 150.0, "g", 195.0, 43.0, 4.0, 0.4);

        when(mealService.addFoodToMeal(eq(10L), any(AddFoodToMealRequest.class), eq(1L)))
                .thenReturn(response);

        mockMvc.perform(post("/meals/10/foods")
                        .with(authentication(authAs(1L))).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.foodName").value("Arroz"))
                .andExpect(jsonPath("$.calories").value(195.0));
    }

    @Test
    @DisplayName("POST /meals/{mealId}/foods — deve retornar 400 quando foodName é vazio")
    void addFoodToMeal_shouldReturn400WhenFoodNameBlank() throws Exception {
        AddFoodToMealRequest request = new AddFoodToMealRequest("", 150.0, "g");

        mockMvc.perform(post("/meals/10/foods")
                        .with(authentication(authAs(1L))).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /meals/{mealId}/foods — deve retornar 400 quando quantity é negativa")
    void addFoodToMeal_shouldReturn400WhenQuantityNegative() throws Exception {
        AddFoodToMealRequest request = new AddFoodToMealRequest("Arroz", -1.0, "g");

        mockMvc.perform(post("/meals/10/foods")
                        .with(authentication(authAs(1L))).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // ── getMealSummary ────────────────────────────────────────────────────────

    @Test
    @DisplayName("GET /meals/{mealId}/summary — deve retornar resumo da refeição")
    void getMealSummary_shouldReturn200() throws Exception {
        MealSummaryResponse response = new MealSummaryResponse(
                10L, 550.0, 35.0, 70.0, 17.0, 2.0);

        when(mealService.getMealSummary(eq(10L), eq(1L))).thenReturn(response);

        mockMvc.perform(get("/meals/10/summary")
                        .with(authentication(authAs(1L))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mealId").value(10))
                .andExpect(jsonPath("$.totalCalories").value(550.0))
                .andExpect(jsonPath("$.foodCount").value(2.0));
    }

    @Test
    @DisplayName("GET /meals/{mealId}/summary — deve retornar 404 quando refeição não existe")
    void getMealSummary_shouldReturn404WhenNotFound() throws Exception {
        when(mealService.getMealSummary(eq(99L), eq(1L)))
                .thenThrow(new ResourceNotFoundException("Meal not found!"));

        mockMvc.perform(get("/meals/99/summary")
                        .with(authentication(authAs(1L))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Meal not found!"));
    }

    // ── deleteMeal ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("DELETE /meals/{mealId} — deve deletar refeição e retornar 204")
    void deleteMeal_shouldReturn204() throws Exception {
        doNothing().when(mealService).DeleteMeal(eq(10L), eq(1L));

        mockMvc.perform(delete("/meals/10")
                        .with(authentication(authAs(1L))).with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("DELETE /meals/{mealId} — deve retornar 404 quando refeição não existe")
    void deleteMeal_shouldReturn404WhenNotFound() throws Exception {
        doThrow(new ResourceNotFoundException("Meal not found!"))
                .when(mealService).DeleteMeal(eq(99L), eq(1L));

        mockMvc.perform(delete("/meals/99")
                        .with(authentication(authAs(1L))).with(csrf()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Meal not found!"));
    }

    // ── deleteMealFood ────────────────────────────────────────────────────────

    @Test
    @DisplayName("DELETE /meals/{mealId}/foods/{mealFoodId} — deve deletar alimento e retornar 204")
    void deleteMealFood_shouldReturn204() throws Exception {
        doNothing().when(mealService).DeleteMealFood(eq(10L), eq(1L), eq(1L));

        mockMvc.perform(delete("/meals/10/foods/1")
                        .with(authentication(authAs(1L))).with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("DELETE /meals/{mealId}/foods/{mealFoodId} — deve retornar 404 quando alimento não existe")
    void deleteMealFood_shouldReturn404WhenNotFound() throws Exception {
        doThrow(new ResourceNotFoundException("MealFood not found!"))
                .when(mealService).DeleteMealFood(eq(10L), eq(99L), eq(1L));

        mockMvc.perform(delete("/meals/10/foods/99")
                        .with(authentication(authAs(1L))).with(csrf()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("MealFood not found!"));
    }

    // ── updateMealFoodQuantity ────────────────────────────────────────────────

    @Test
    @DisplayName("PATCH /meals/{mealId}/foods/{mealFoodId} — deve atualizar quantidade e retornar 200")
    void updateMealFoodQuantity_shouldReturn200() throws Exception {
        UpdateMealFoodQuantityRequest request = new UpdateMealFoodQuantityRequest(200.0);
        MealFoodResponse response = new MealFoodResponse(
                1L, "Arroz", 200.0, "g", 260.0, 57.3, 5.3, 0.5);

        when(mealService.updateMealFoodQuantity(eq(10L), eq(1L), eq(200.0), eq(1L)))
                .thenReturn(response);

        mockMvc.perform(patch("/meals/10/foods/1")
                        .with(authentication(authAs(1L))).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantity").value(200.0))
                .andExpect(jsonPath("$.calories").value(260.0));
    }

    @Test
    @DisplayName("PATCH /meals/{mealId}/foods/{mealFoodId} — deve retornar 400 quando quantidade é negativa")
    void updateMealFoodQuantity_shouldReturn400WhenQuantityNegative() throws Exception {
        UpdateMealFoodQuantityRequest request = new UpdateMealFoodQuantityRequest(-1.0);

        mockMvc.perform(patch("/meals/10/foods/1")
                        .with(authentication(authAs(1L))).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
