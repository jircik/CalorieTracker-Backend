package com.jircik.calorietrackerapi.domain.dto.response;

public record AuthResponse(
        String token,
        Long userId,
        String name,
        String email
) {
}
