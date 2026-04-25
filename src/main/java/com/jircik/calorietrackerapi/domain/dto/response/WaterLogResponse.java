package com.jircik.calorietrackerapi.domain.dto.response;

import java.time.LocalDateTime;

public record WaterLogResponse(
        Long id,
        Integer amountMl,
        LocalDateTime loggedAt) {
}
