package com.jircik.calorietrackerapi.domain.dto.exception;

import java.time.LocalDateTime;

public record DuplicateMealErrorResponse(
        int status,
        String message,
        Long existingMealId,
        String path,
        LocalDateTime timestamp
) {
}
