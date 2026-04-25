package com.jircik.calorietrackerapi.domain.dto.response;

import java.time.LocalDate;
import java.util.List;

public record DailyWaterResponse(
        Long userId,
        LocalDate date,
        Integer dailyGoalMl,          // from user.dailyWaterGoalMl — null if not set
        Integer totalConsumedMl,
        List<WaterLogResponse> logs
) {
}
