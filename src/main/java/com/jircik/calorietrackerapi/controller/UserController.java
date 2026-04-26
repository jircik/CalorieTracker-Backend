package com.jircik.calorietrackerapi.controller;

import com.jircik.calorietrackerapi.domain.dto.request.ConfigureUserProfileRequest;
import com.jircik.calorietrackerapi.domain.dto.request.GetSummaryRequest;
import com.jircik.calorietrackerapi.domain.dto.response.DailySummaryResponse;
import com.jircik.calorietrackerapi.domain.dto.response.MealsByDateResponse;
import com.jircik.calorietrackerapi.domain.dto.response.SummaryResponse;
import com.jircik.calorietrackerapi.domain.dto.response.UserResponse;
import com.jircik.calorietrackerapi.service.UserService;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@Tag(name = "Users", description = "User management and nutritional summaries")
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /*

    @Operation(summary = "Create a user", description = "Creates a new user with name and email")
    @PostMapping
    public UserResponse createUser(@RequestBody CreateUserRequest request) {
        return userService.createUser(request);
    }
    */

    @Operation(summary = "Update user profile", description = "Partial update — only non-null fields are applied (age, height, weight, goal, gender, activity level)")
    @PreAuthorize("#id == authentication.principal.userId")
    @PatchMapping("/{id}/profile")
    public ResponseEntity<UserResponse> configureUserProfile (
            @RequestBody ConfigureUserProfileRequest request,
            @PathVariable Long id) {
        return ResponseEntity.ok(userService.configureUserProfile(request,id));
    }

    @Operation(summary = "Get user by ID", description = "Returns the full user profile including optional profile fields")
    @PreAuthorize("#id == authentication.principal.userId")
    @GetMapping("/{id}")
    public UserResponse getUser(@PathVariable Long id) {
        return userService.getUser(id);
    }

    @Operation(summary = "Get all users", description = "Returns all users — development endpoint, no authentication guard")
    @GetMapping
    public List<UserResponse> getAllUsers() {
        return userService.getAllUsers();
    }

    // Deprecated Method, will redirect to new getPeriodSummary method
    @PreAuthorize("#userId == authentication.principal.userId")
    @Hidden
    @GetMapping("/{userId}/daily-summary")
    @Deprecated(since = "2026-03", forRemoval = true)
    public ResponseEntity<DailySummaryResponse> getDailySummaryLegacy(
            @PathVariable Long userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        GetSummaryRequest request = new GetSummaryRequest(
                userId,
                date,
                date,
                "DAILY"
        );

        SummaryResponse newResponse = userService.getPeriodSummary(request);

        DailySummaryResponse legacyResponse = new DailySummaryResponse(
                newResponse.userId(),
                newResponse.startDate(),
                newResponse.totalCalories(),
                newResponse.totalProtein(),
                newResponse.totalCarbs(),
                newResponse.totalFat(),
                newResponse.mealCount(),
                newResponse.foodCount()
        );

        return ResponseEntity.ok(legacyResponse);
    }

    @Operation(summary = "Get period summary", description = "Returns aggregated nutritional totals for a date range. Supports DAILY, WEEKLY (auto-adjusts to Mon–Sun), MONTHLY (auto-adjusts to full month), and CUSTOM (explicit startDate + endDate required). Maximum range: 366 days.")
    @PreAuthorize("#userId == authentication.principal.userId")
    @GetMapping("/{userId}/summary")
    public ResponseEntity<SummaryResponse> getPeriodSummary(
            @PathVariable Long userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String periodType) {
        GetSummaryRequest request = new GetSummaryRequest(
                userId,
                startDate,
                endDate,
                periodType
        );

        SummaryResponse response = userService.getPeriodSummary(request);

        return ResponseEntity.ok(response);
    }


    @Operation(summary = "Get meals by date", description = "Returns meals grouped by meal type (BREAKFAST, LUNCH, DINNER, SNACKS) for a given date. All four keys are always present; types with no meal on that date are null.")
    @PreAuthorize("#userId == authentication.principal.userId")
    @GetMapping("/{userId}/meals")
    public MealsByDateResponse getMealsByDate(
            @PathVariable Long userId,
            @RequestParam LocalDate date
    ) {
        return userService.getMealsByDate(userId, date);
    }
}
