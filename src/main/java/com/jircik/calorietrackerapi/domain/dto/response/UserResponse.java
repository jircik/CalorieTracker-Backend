package com.jircik.calorietrackerapi.domain.dto.response;

import com.jircik.calorietrackerapi.domain.entity.ActivityLevelEnum;
import com.jircik.calorietrackerapi.domain.entity.GenderEnum;
import io.swagger.v3.oas.annotations.media.Schema;

public record UserResponse(

        @Schema(description = "Unique user ID")
        Long id,

        @Schema(description = "User's full name")
        String name,

        @Schema(description = "User's email address")
        String email,

        @Schema(description = "Age in years; null if profile not configured")
        Integer age,

        @Schema(description = "Height in centimeters; null if profile not configured")
        Integer heightInCm,

        @Schema(description = "Current body weight in kilograms; null if profile not configured")
        Double currentWeight,

        @Schema(description = "Target body weight in kilograms; null if profile not configured")
        Double weightGoal,

        @Schema(description = "Daily calorie intake goal in kcal; null if profile not configured")
        Double dailyCalorieIntakeGoal,

        @Schema(description = "Daily water intake goal in milliliters; null if not configured")
        Integer dailyWaterGoalMl,

        @Schema(description = "User's gender; null if profile not configured")
        GenderEnum gender,

        @Schema(description = "User's physical activity level; null if profile not configured")
        ActivityLevelEnum activityLevel

) {}
