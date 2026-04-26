package com.jircik.calorietrackerapi.controller;

import com.jircik.calorietrackerapi.domain.dto.request.LogWaterRequest;
import com.jircik.calorietrackerapi.domain.dto.response.DailyWaterResponse;
import com.jircik.calorietrackerapi.domain.dto.response.WaterLogResponse;
import com.jircik.calorietrackerapi.service.WaterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@Tag(name = "Water", description = "Daily water intake tracking")
@RestController
@RequestMapping("/users/{userId}/water")
@RequiredArgsConstructor
public class WaterController {

    private final WaterService waterService;

    @Operation(summary = "Log water intake", description = "Adds a water consumption entry for the user")
    @PreAuthorize("#userId == authentication.principal.userId")
    @PostMapping
    public ResponseEntity<WaterLogResponse> logWater(
            @PathVariable Long userId,
            @RequestBody @Valid LogWaterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(waterService.logWater(userId, request));
    }

    @Operation(summary = "Get daily water intake", description = "Returns all water logs for a given date and the daily total")
    @PreAuthorize("#userId == authentication.principal.userId")
    @GetMapping
    public ResponseEntity<DailyWaterResponse> getDailyWater(
            @PathVariable Long userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(waterService.getDailyWater(userId, date));
    }

    @Operation(summary = "Delete a water log entry")
    @PreAuthorize("#userId == authentication.principal.userId")
    @DeleteMapping("/{logId}")
    public ResponseEntity<Void> deleteWaterLog(
            @PathVariable Long userId,
            @PathVariable Long logId) {
        waterService.deleteWaterLog(userId, logId);
        return ResponseEntity.noContent().build();
    }
}