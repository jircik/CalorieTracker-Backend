package com.jircik.calorietrackerapi.repository;

import com.jircik.calorietrackerapi.domain.entity.Meal;
import com.jircik.calorietrackerapi.domain.entity.MealTypeEnum;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface MealRepository extends JpaRepository<Meal, Long> {

    List<Meal> findByUser_IdAndDatetimeBetweenOrderByDatetimeAsc(
            Long userId,
            LocalDateTime start,
            LocalDateTime end
    );

    Optional<Meal> findByUser_IdAndMealTypeAndDatetimeBetween(
            Long userId,
            MealTypeEnum mealType,
            LocalDateTime start,
            LocalDateTime end
    );

}
