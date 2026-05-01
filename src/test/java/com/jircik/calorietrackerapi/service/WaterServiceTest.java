package com.jircik.calorietrackerapi.service;

import com.jircik.calorietrackerapi.domain.dto.request.LogWaterRequest;
import com.jircik.calorietrackerapi.domain.dto.response.DailyWaterResponse;
import com.jircik.calorietrackerapi.domain.dto.response.WaterLogResponse;
import com.jircik.calorietrackerapi.domain.entity.User;
import com.jircik.calorietrackerapi.domain.entity.WaterLog;
import com.jircik.calorietrackerapi.exception.ResourceNotFoundException;
import com.jircik.calorietrackerapi.repository.UserRepository;
import com.jircik.calorietrackerapi.repository.WaterLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WaterServiceTest {

    @Mock
    private WaterLogRepository waterLogRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private WaterService waterService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L).name("João").email("joao@email.com")
                .dailyWaterGoalMl(2000)
                .build();
    }

    // ── logWater ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("logWater — deve persistir e retornar response")
    void logWater_shouldPersistAndReturn() {
        LogWaterRequest request = new LogWaterRequest(300);
        LocalDateTime loggedAt = LocalDateTime.of(2026, 5, 1, 10, 0);
        WaterLog saved = WaterLog.builder()
                .id(99L).user(testUser).amountMl(300).loggedAt(loggedAt).build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(waterLogRepository.save(any(WaterLog.class))).thenReturn(saved);

        WaterLogResponse response = waterService.logWater(1L, request);

        assertThat(response.id()).isEqualTo(99L);
        assertThat(response.amountMl()).isEqualTo(300);
        assertThat(response.loggedAt()).isEqualTo(loggedAt);
        verify(waterLogRepository).save(any(WaterLog.class));
    }

    @Test
    @DisplayName("logWater — deve lançar exceção quando usuário não existe")
    void logWater_shouldThrowWhenUserNotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> waterService.logWater(99L, new LogWaterRequest(300)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("User not found");

        verify(waterLogRepository, never()).save(any());
    }

    // ── getDailyWater ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("getDailyWater — deve somar logs e mapear para response")
    void getDailyWater_shouldSumAndMap() {
        LocalDate date = LocalDate.of(2026, 5, 1);
        WaterLog log1 = WaterLog.builder()
                .id(1L).user(testUser).amountMl(300)
                .loggedAt(date.atTime(8, 0)).build();
        WaterLog log2 = WaterLog.builder()
                .id(2L).user(testUser).amountMl(500)
                .loggedAt(date.atTime(14, 30)).build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(waterLogRepository.findByUser_IdAndLoggedAtBetweenOrderByLoggedAtAsc(
                1L, date.atStartOfDay(), date.atTime(LocalTime.MAX)))
                .thenReturn(List.of(log1, log2));

        DailyWaterResponse response = waterService.getDailyWater(1L, date);

        assertThat(response.userId()).isEqualTo(1L);
        assertThat(response.date()).isEqualTo(date);
        assertThat(response.dailyGoalMl()).isEqualTo(2000);
        assertThat(response.totalConsumedMl()).isEqualTo(800);
        assertThat(response.logs()).hasSize(2);
        assertThat(response.logs().get(0).amountMl()).isEqualTo(300);
    }

    @Test
    @DisplayName("getDailyWater — deve retornar zero e lista vazia quando não há logs")
    void getDailyWater_shouldReturnZeroWhenNoLogs() {
        LocalDate date = LocalDate.of(2026, 5, 1);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(waterLogRepository.findByUser_IdAndLoggedAtBetweenOrderByLoggedAtAsc(
                any(), any(), any())).thenReturn(List.of());

        DailyWaterResponse response = waterService.getDailyWater(1L, date);

        assertThat(response.totalConsumedMl()).isZero();
        assertThat(response.logs()).isEmpty();
    }

    @Test
    @DisplayName("getDailyWater — deve preservar dailyGoalMl null quando usuário não configurou")
    void getDailyWater_shouldHandleNullGoal() {
        User noGoal = User.builder().id(2L).name("Maria").email("maria@email.com").build();
        when(userRepository.findById(2L)).thenReturn(Optional.of(noGoal));
        when(waterLogRepository.findByUser_IdAndLoggedAtBetweenOrderByLoggedAtAsc(
                any(), any(), any())).thenReturn(List.of());

        DailyWaterResponse response = waterService.getDailyWater(2L, LocalDate.of(2026, 5, 1));

        assertThat(response.dailyGoalMl()).isNull();
    }

    @Test
    @DisplayName("getDailyWater — deve lançar exceção quando usuário não existe")
    void getDailyWater_shouldThrowWhenUserNotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> waterService.getDailyWater(99L, LocalDate.now()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("User not found");
    }

    // ── deleteWaterLog ────────────────────────────────────────────────────────

    @Test
    @DisplayName("deleteWaterLog — deve deletar quando log existe e pertence ao usuário")
    void deleteWaterLog_shouldDeleteWhenOwner() {
        WaterLog log = WaterLog.builder().id(5L).user(testUser).amountMl(250).build();
        when(waterLogRepository.findById(5L)).thenReturn(Optional.of(log));

        waterService.deleteWaterLog(1L, 5L);

        verify(waterLogRepository).delete(log);
    }

    @Test
    @DisplayName("deleteWaterLog — deve lançar exceção quando log não existe")
    void deleteWaterLog_shouldThrowWhenNotFound() {
        when(waterLogRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> waterService.deleteWaterLog(1L, 99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Water log not found");

        verify(waterLogRepository, never()).delete(any());
    }

    @Test
    @DisplayName("deleteWaterLog — deve lançar exceção quando log pertence a outro usuário")
    void deleteWaterLog_shouldThrowWhenNotOwner() {
        User other = User.builder().id(2L).build();
        WaterLog log = WaterLog.builder().id(5L).user(other).amountMl(250).build();
        when(waterLogRepository.findById(5L)).thenReturn(Optional.of(log));

        assertThatThrownBy(() -> waterService.deleteWaterLog(1L, 5L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Water log not found");

        verify(waterLogRepository, never()).delete(any());
    }
}
