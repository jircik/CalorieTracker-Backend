package com.jircik.calorietrackerapi.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.jircik.calorietrackerapi.domain.dto.request.LogWaterRequest;
import com.jircik.calorietrackerapi.domain.dto.response.DailyWaterResponse;
import com.jircik.calorietrackerapi.domain.dto.response.WaterLogResponse;
import com.jircik.calorietrackerapi.exception.ResourceNotFoundException;
import com.jircik.calorietrackerapi.security.JwtService;
import com.jircik.calorietrackerapi.service.WaterService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static com.jircik.calorietrackerapi.util.SecurityTestUtils.authAs;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(WaterController.class)
@Import(WaterControllerTest.MethodSecurityConfig.class)
class WaterControllerTest {

    @EnableMethodSecurity
    static class MethodSecurityConfig {}

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    @MockitoBean
    private WaterService waterService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    // ── logWater ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("POST /users/{id}/water — deve criar log e retornar 201")
    void logWater_shouldReturn201() throws Exception {
        LogWaterRequest request = new LogWaterRequest(300);
        WaterLogResponse response = new WaterLogResponse(
                10L, 300, LocalDateTime.of(2026, 5, 1, 10, 0));
        when(waterService.logWater(eq(1L), any(LogWaterRequest.class))).thenReturn(response);

        mockMvc.perform(post("/users/1/water")
                        .with(authentication(authAs(1L))).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.amountMl").value(300));
    }

    @Test
    @DisplayName("POST /users/{id}/water — deve retornar 400 quando amountMl é nulo")
    void logWater_shouldReturn400WhenAmountNull() throws Exception {
        LogWaterRequest request = new LogWaterRequest(null);

        mockMvc.perform(post("/users/1/water")
                        .with(authentication(authAs(1L))).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /users/{id}/water — deve retornar 400 quando amountMl é zero")
    void logWater_shouldReturn400WhenAmountZero() throws Exception {
        LogWaterRequest request = new LogWaterRequest(0);

        mockMvc.perform(post("/users/1/water")
                        .with(authentication(authAs(1L))).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /users/{id}/water — deve retornar 403 quando userId no path difere do principal")
    void logWater_shouldReturn403ForOtherUser() throws Exception {
        LogWaterRequest request = new LogWaterRequest(300);

        mockMvc.perform(post("/users/2/water")
                        .with(authentication(authAs(1L))).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    // ── getDailyWater ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("GET /users/{id}/water — deve retornar resumo diário")
    void getDailyWater_shouldReturn200() throws Exception {
        LocalDate date = LocalDate.of(2026, 5, 1);
        DailyWaterResponse response = new DailyWaterResponse(
                1L, date, 2000, 800,
                List.of(new WaterLogResponse(1L, 300, date.atTime(8, 0)),
                        new WaterLogResponse(2L, 500, date.atTime(14, 30))));
        when(waterService.getDailyWater(eq(1L), eq(date))).thenReturn(response);

        mockMvc.perform(get("/users/1/water")
                        .param("date", "2026-05-01")
                        .with(authentication(authAs(1L))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.totalConsumedMl").value(800))
                .andExpect(jsonPath("$.dailyGoalMl").value(2000))
                .andExpect(jsonPath("$.logs.length()").value(2));
    }

    @Test
    @DisplayName("GET /users/{id}/water — deve retornar 404 quando usuário não existe")
    void getDailyWater_shouldReturn404WhenUserNotFound() throws Exception {
        when(waterService.getDailyWater(eq(99L), any()))
                .thenThrow(new ResourceNotFoundException("User not found"));

        mockMvc.perform(get("/users/99/water")
                        .param("date", "2026-05-01")
                        .with(authentication(authAs(99L))))
                .andExpect(status().isNotFound());
    }

    // ── deleteWaterLog ────────────────────────────────────────────────────────

    @Test
    @DisplayName("DELETE /users/{id}/water/{logId} — deve deletar e retornar 204")
    void deleteWaterLog_shouldReturn204() throws Exception {
        doNothing().when(waterService).deleteWaterLog(eq(1L), eq(5L));

        mockMvc.perform(delete("/users/1/water/5")
                        .with(authentication(authAs(1L))).with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("DELETE /users/{id}/water/{logId} — deve retornar 404 quando log não existe")
    void deleteWaterLog_shouldReturn404() throws Exception {
        doThrow(new ResourceNotFoundException("Water log not found"))
                .when(waterService).deleteWaterLog(eq(1L), eq(99L));

        mockMvc.perform(delete("/users/1/water/99")
                        .with(authentication(authAs(1L))).with(csrf()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Water log not found"));
    }
}
