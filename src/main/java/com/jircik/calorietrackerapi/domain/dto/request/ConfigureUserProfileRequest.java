package com.jircik.calorietrackerapi.domain.dto.request;

import com.jircik.calorietrackerapi.domain.entity.ActivityLevelEnum;
import com.jircik.calorietrackerapi.domain.entity.GenderEnum;
import io.swagger.v3.oas.annotations.media.Schema;

public record ConfigureUserProfileRequest(

        @Schema(description = "Age in years", example = "25")
        Integer age,

        @Schema(description = "Height in centimeters", example = "175")
        Integer heightInCm,

        @Schema(description = "Current body weight in kilograms", example = "75.0")
        Double currentWeight,

        @Schema(description = "Target body weight in kilograms", example = "70.0")
        Double weightGoal,

        @Schema(description = "Daily calorie intake goal in kcal", example = "2200.0")
        Double dailyCalorieIntakeGoal,

        @Schema(description = "Daily water intake goal in milliliters", example = "2500")
        Integer dailyWaterGoalMl,

        @Schema(description = "User's gender")
        GenderEnum gender,

        @Schema(description = "User's physical activity level")
        ActivityLevelEnum activityLevel

) {}