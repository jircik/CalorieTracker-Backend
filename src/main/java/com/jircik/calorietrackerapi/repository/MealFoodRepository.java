package com.jircik.calorietrackerapi.repository;

import com.jircik.calorietrackerapi.domain.entity.MealFood;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface MealFoodRepository extends JpaRepository<MealFood, Long> {
    List<MealFood> findByMeal_Id(Long mealId);
    List<MealFood> findByMeal_IdInOrderByCreatedAtAsc(List<Long> mealIds);
    List<MealFood> findByMeal_User_IdAndMeal_DatetimeBetween(Long userId, LocalDateTime start, LocalDateTime end);
    Optional<MealFood> findByIdAndMeal_Id(Long mealFoodId, Long mealId);
}