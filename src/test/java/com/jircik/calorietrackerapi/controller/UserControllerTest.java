package com.jircik.calorietrackerapi.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.jircik.calorietrackerapi.domain.dto.request.ConfigureUserProfileRequest;
import com.jircik.calorietrackerapi.domain.dto.request.CreateUserRequest;
import com.jircik.calorietrackerapi.domain.dto.request.GetSummaryRequest;
import com.jircik.calorietrackerapi.domain.dto.response.SummaryResponse;
import com.jircik.calorietrackerapi.domain.dto.response.MealsByDateResponse;
import com.jircik.calorietrackerapi.domain.dto.response.UserResponse;
import com.jircik.calorietrackerapi.exception.ResourceNotFoundException;
import com.jircik.calorietrackerapi.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    @MockitoBean
    private UserService userService;

    @Test
    @DisplayName("POST /users — deve criar usuário e retornar 200")
    void createUser_shouldReturn200() throws Exception {
        CreateUserRequest request = new CreateUserRequest("João", "joao@email.com");
        UserResponse response = new UserResponse(1L, "João", "joao@email.com", null, null, null, null, null, null, null, null);

        when(userService.createUser(any(CreateUserRequest.class))).thenReturn(response);

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("João"))
                .andExpect(jsonPath("$.email").value("joao@email.com"));
    }

    @Test
    @DisplayName("GET /users/{id} — deve retornar usuário quando encontrado")
    void getUser_shouldReturn200WhenFound() throws Exception {
        UserResponse response = new UserResponse(1L, "João", "joao@email.com", null, null, null, null, null, null, null, null);
        when(userService.getUser(1L)).thenReturn(response);

        mockMvc.perform(get("/users/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("João"));
    }

    @Test
    @DisplayName("GET /users/{id} — deve retornar 404 quando não encontrado")
    void getUser_shouldReturn404WhenNotFound() throws Exception {
        when(userService.getUser(99L))
                .thenThrow(new ResourceNotFoundException("User not found"));

        mockMvc.perform(get("/users/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("User not found"));
    }

    @Test
    @DisplayName("GET /users — deve retornar lista de usuários")
    void getAllUsers_shouldReturnList() throws Exception {
        List<UserResponse> responses = List.of(
                new UserResponse(1L, "João", "joao@email.com", null, null, null, null, null, null, null, null),
                new UserResponse(2L, "Maria", "maria@email.com", null, null, null, null, null, null, null, null)
        );
        when(userService.getAllUsers()).thenReturn(responses);

        mockMvc.perform(get("/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("João"))
                .andExpect(jsonPath("$[1].name").value("Maria"));
    }

    @Test
    @DisplayName("GET /users — deve retornar lista vazia")
    void getAllUsers_shouldReturnEmptyList() throws Exception {
        when(userService.getAllUsers()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("GET /users/{userId}/daily-summary — deve retornar resumo diário (legado)")
    void getDailySummary_shouldReturn200() throws Exception {
        LocalDate date = LocalDate.of(2026, 3, 10);
        SummaryResponse summaryResponse = new SummaryResponse(
                1L, date, date, "DAILY", 700.0, 43.0, 90.0, 20.0, 2L, 3L, 1, 700.0);

        when(userService.getPeriodSummary(any(GetSummaryRequest.class))).thenReturn(summaryResponse);

        mockMvc.perform(get("/users/1/daily-summary")
                        .param("date", "2026-03-10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.totalCalories").value(700.0))
                .andExpect(jsonPath("$.mealCount").value(2));
    }

    @Test
    @DisplayName("PATCH /users/{id}/profile — deve atualizar perfil e retornar 200")
    void configureUserProfile_shouldReturn200() throws Exception {
        ConfigureUserProfileRequest request = new ConfigureUserProfileRequest(
                25, 1.75, 75.0, 70.0, 2200.0, null, null, null);
        UserResponse response = new UserResponse(1L, "João", "joao@email.com",
                25, 1.75, 75.0, 70.0, 2200.0, null, null, null);

        when(userService.configureUserProfile(any(ConfigureUserProfileRequest.class), eq(1L)))
                .thenReturn(response);

        mockMvc.perform(patch("/users/1/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.age").value(25))
                .andExpect(jsonPath("$.heightInMeters").value(1.75));
    }

    @Test
    @DisplayName("PATCH /users/{id}/profile — deve retornar 404 quando usuário não existe")
    void configureUserProfile_shouldReturn404WhenNotFound() throws Exception {
        ConfigureUserProfileRequest request = new ConfigureUserProfileRequest(
                25, null, null, null, null, null, null, null);

        when(userService.configureUserProfile(any(ConfigureUserProfileRequest.class), eq(99L)))
                .thenThrow(new ResourceNotFoundException("User not found"));

        mockMvc.perform(patch("/users/99/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("User not found"));
    }

    @Test
    @DisplayName("GET /users/{userId}/summary — deve retornar resumo do período")
    void getPeriodSummary_shouldReturn200() throws Exception {
        LocalDate startDate = LocalDate.of(2026, 4, 9);
        SummaryResponse response = new SummaryResponse(
                1L, startDate, startDate, "DAILY", 500.0, 30.0, 60.0, 15.0, 1L, 3L, 1, 500.0);

        when(userService.getPeriodSummary(any(GetSummaryRequest.class))).thenReturn(response);

        mockMvc.perform(get("/users/1/summary")
                        .param("startDate", "2026-04-09")
                        .param("periodType", "DAILY"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.periodType").value("DAILY"))
                .andExpect(jsonPath("$.totalCalories").value(500.0));
    }

    @Test
    @DisplayName("GET /users/{userId}/summary — deve retornar 404 quando usuário não existe")
    void getPeriodSummary_shouldReturn404WhenUserNotFound() throws Exception {
        when(userService.getPeriodSummary(any(GetSummaryRequest.class)))
                .thenThrow(new ResourceNotFoundException("Usuário não encontrado"));

        mockMvc.perform(get("/users/99/summary")
                        .param("startDate", "2026-04-09")
                        .param("periodType", "DAILY"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Usuário não encontrado"));
    }

    @Test
    @DisplayName("GET /users/{userId}/meals — deve retornar refeições por data")
    void getMealsByDate_shouldReturn200() throws Exception {
        LocalDate date = LocalDate.of(2026, 3, 10);
        MealsByDateResponse response = new MealsByDateResponse(
                1L, date, Collections.emptyMap());

        when(userService.getMealsByDate(eq(1L), eq(date))).thenReturn(response);

        mockMvc.perform(get("/users/1/meals")
                        .param("date", "2026-03-10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.date").value("2026-03-10"));
    }
}
