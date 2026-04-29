package com.jircik.calorietrackerapi.controller;

import com.jircik.calorietrackerapi.domain.dto.response.FoodSearchResultResponse;
import com.jircik.calorietrackerapi.service.FoodService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Foods", description = "Food search via FatSecret")
@RestController
@RequestMapping("/foods")
@RequiredArgsConstructor
public class FoodController {

    private final FoodService foodService;

    @Operation(
            summary = "Search foods",
            description = "Returns up to 5 matching foods from FatSecret. Pass the foodId from the result to POST /meals/{mealId}/foods.")
    @GetMapping("/search")
    public ResponseEntity<List<FoodSearchResultResponse>> searchFoods(@RequestParam String q) {
        return ResponseEntity.ok(foodService.searchFoods(q));
    }
}