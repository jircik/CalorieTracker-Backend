package com.jircik.calorietrackerapi.service;

import com.jircik.calorietrackerapi.domain.dto.request.LogWaterRequest;
import com.jircik.calorietrackerapi.domain.dto.response.DailyWaterResponse;
import com.jircik.calorietrackerapi.domain.dto.response.WaterLogResponse;
import com.jircik.calorietrackerapi.domain.entity.User;
import com.jircik.calorietrackerapi.domain.entity.WaterLog;
import com.jircik.calorietrackerapi.exception.ResourceNotFoundException;
import com.jircik.calorietrackerapi.repository.UserRepository;
import com.jircik.calorietrackerapi.repository.WaterLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class WaterService {

    private final WaterLogRepository waterLogRepository;
    private final UserRepository userRepository;

    public WaterLogResponse logWater(Long userId, LogWaterRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        WaterLog log = WaterLog.builder()
                .user(user)
                .amountMl(request.amountMl())
                .build();

        WaterLog saved = waterLogRepository.save(log);

        return new WaterLogResponse(saved.getId(), saved.getAmountMl(), saved.getLoggedAt());
    }

    public DailyWaterResponse getDailyWater(Long userId, LocalDate date) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        LocalDateTime from = date.atStartOfDay();
        LocalDateTime to   = date.atTime(LocalTime.MAX);

        List<WaterLog> logs = waterLogRepository
                .findByUser_IdAndLoggedAtBetweenOrderByLoggedAtAsc(userId, from, to);

        int totalMl = logs.stream().mapToInt(WaterLog::getAmountMl).sum();

        List<WaterLogResponse> logResponses = logs.stream()
                .map(l -> new WaterLogResponse(l.getId(), l.getAmountMl(), l.getLoggedAt()))
                .toList();

        return new DailyWaterResponse(userId, date, user.getDailyWaterGoalMl(), totalMl, logResponses);
    }

    public void deleteWaterLog(Long userId, Long logId) {
        WaterLog log = waterLogRepository.findById(logId)
                .orElseThrow(() -> new ResourceNotFoundException("Water log not found"));

        if (!log.getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("Water log not found");
        }

        waterLogRepository.delete(log);
    }
}