package com.jircik.calorietrackerapi.controller;

import com.jircik.calorietrackerapi.domain.dto.response.FoodSearchResultResponse;
import com.jircik.calorietrackerapi.security.JwtService;
import com.jircik.calorietrackerapi.service.FoodService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static com.jircik.calorietrackerapi.util.SecurityTestUtils.authAs;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(FoodController.class)
class FoodControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FoodService foodService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @Test
    @DisplayName("GET /foods/search — deve retornar lista de resultados")
    void searchFoods_shouldReturn200WithResults() throws Exception {
        List<FoodSearchResultResponse> results = List.of(
                new FoodSearchResultResponse("123", "Arroz Branco", null, "Per 100g: 130 kcal"),
                new FoodSearchResultResponse("456", "Arroz Integral", null, "Per 100g: 111 kcal")
        );
        when(foodService.searchFoods("arroz")).thenReturn(results);

        mockMvc.perform(get("/foods/search")
                        .param("q", "arroz")
                        .with(authentication(authAs(1L))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].foodId").value("123"))
                .andExpect(jsonPath("$[0].foodName").value("Arroz Branco"));
    }

    @Test
    @DisplayName("GET /foods/search — deve retornar 400 quando query está em branco")
    void searchFoods_shouldReturn400WhenQueryBlank() throws Exception {
        when(foodService.searchFoods(""))
                .thenThrow(new IllegalArgumentException("Search query must not be blank"));

        mockMvc.perform(get("/foods/search")
                        .param("q", "")
                        .with(authentication(authAs(1L))))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /foods/search — deve retornar 403 sem token")
    void searchFoods_shouldReturn403WithoutAuth() throws Exception {
        mockMvc.perform(get("/foods/search").param("q", "arroz"))
                .andExpect(status().isForbidden());
    }
}
