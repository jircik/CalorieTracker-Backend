package com.jircik.calorietrackerapi.service;

import com.jircik.calorietrackerapi.domain.dto.request.AddFoodToMealRequest;
import com.jircik.calorietrackerapi.domain.dto.response.MealFoodResponse;
import com.jircik.calorietrackerapi.domain.dto.response.MealResponse;
import com.jircik.calorietrackerapi.domain.dto.response.MealSummaryResponse;
import com.jircik.calorietrackerapi.domain.entity.Meal;
import com.jircik.calorietrackerapi.domain.entity.MealFood;
import com.jircik.calorietrackerapi.domain.entity.MealTypeEnum;
import com.jircik.calorietrackerapi.domain.entity.User;
import com.jircik.calorietrackerapi.domain.fatsecret.NutritionProvider;
import com.jircik.calorietrackerapi.domain.fatsecret.NutritionResult;
import com.jircik.calorietrackerapi.exception.ResourceNotFoundException;
import com.jircik.calorietrackerapi.repository.MealFoodRepository;
import com.jircik.calorietrackerapi.repository.MealRepository;
import com.jircik.calorietrackerapi.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class MealService {
    private final MealRepository mealRepository;
    private final UserRepository userRepository;
    private final MealFoodRepository mealFoodRepository;
    private final NutritionProvider nutritionProvider;

    public MealService(
            MealRepository mealRepository,
            UserRepository userRepository,
            MealFoodRepository mealFoodRepository,
            NutritionProvider nutritionProvider) {
        this.mealRepository = mealRepository;
        this.userRepository = userRepository;
        this.mealFoodRepository = mealFoodRepository;
        this.nutritionProvider = nutritionProvider;
    }

    private void verifyMealOwnership(Meal meal, Long callerUserId) {
        if (!meal.getUser().getId().equals(callerUserId)) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.FORBIDDEN, "Access denied");
        }
    }

    public MealResponse createMeal(Long userId, LocalDateTime date, MealTypeEnum mealType) {
        Meal meal = new Meal();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        meal.setUser(user);
        meal.setDatetime(date);
        meal.setMealType(mealType);

        Meal created = mealRepository.save(meal);

        return new MealResponse(
                created.getId(),
                created.getUser().getId(),
                created.getDatetime(),
                created.getMealType(),
                created.getCreatedAt()
        );
    }

    public MealFoodResponse addFoodToMeal(Long mealId, AddFoodToMealRequest request, Long callerUserId) {
        Meal meal = mealRepository.findById(mealId)
                .orElseThrow(() -> new ResourceNotFoundException("Meal not found"));
        verifyMealOwnership(meal, callerUserId);

        NutritionResult nutrition = nutritionProvider.calculateNutritionByFoodId(
                request.foodId(), request.quantity());

        MealFood mealFood = MealFood.builder()
                .meal(meal)
                .foodName(request.foodName())
                .fatSecretFoodId(request.foodId())
                .quantity(request.quantity())
                .unit(request.unit())
                .calories(nutrition.calories())
                .carbs(nutrition.carbs())
                .protein(nutrition.protein())
                .fat(nutrition.fat())
                .build();

        MealFood saved = mealFoodRepository.save(mealFood);

        return new MealFoodResponse(
                saved.getId(),
                saved.getFoodName(),
                saved.getQuantity(),
                saved.getUnit(),
                saved.getCalories(),
                saved.getCarbs(),
                saved.getProtein(),
                saved.getFat());
    }

    public MealSummaryResponse getMealSummary(Long mealId, Long callerUserId) {
        Meal meal = mealRepository.findById(mealId)
                .orElseThrow(() -> new ResourceNotFoundException("Meal not found!"));
        verifyMealOwnership(meal, callerUserId);
        List<MealFood> foods = mealFoodRepository.findByMeal_Id(mealId);

        Double totalCalories = foods.stream()
                .mapToDouble(MealFood::getCalories)
                .sum();

        Double totalProtein = foods.stream()
                .mapToDouble(MealFood::getProtein)
                .sum();

        Double totalCarbs = foods.stream()
                .mapToDouble(MealFood::getCarbs)
                .sum();

        Double totalFat = foods.stream()
                .mapToDouble(MealFood::getFat)
                .sum();

        Double totalFoods = (double) foods.size();

        return new MealSummaryResponse(
                mealId,
                totalCalories,
                totalProtein,
                totalCarbs,
                totalFat,
                totalFoods
        );
    }

    public void DeleteMeal(Long mealId, Long callerUserId) {
        Meal meal = mealRepository.findById(mealId)
                .orElseThrow(() -> new ResourceNotFoundException("Meal not found!"));
        verifyMealOwnership(meal, callerUserId);
        mealRepository.deleteById(mealId);
    }

    public void DeleteMealFood(Long mealId, Long mealFoodId, Long callerUserId) {
        Meal meal = mealRepository.findById(mealId)
                .orElseThrow(() -> new ResourceNotFoundException("Meal not found!"));
        verifyMealOwnership(meal, callerUserId);
        MealFood mealFood = mealFoodRepository
                .findByIdAndMeal_Id(mealFoodId, mealId)
                .orElseThrow(() -> new ResourceNotFoundException("MealFood not found!"));

        mealFoodRepository.delete(mealFood);
    }

    public MealFoodResponse updateMealFoodQuantity(Long mealId, Long mealFoodId, Double quantity, Long callerUserId) {
        Meal meal = mealRepository.findById(mealId)
                .orElseThrow(() -> new ResourceNotFoundException("Meal not found!"));
        verifyMealOwnership(meal, callerUserId);

        if (quantity == null || quantity <= 0 ) {
            throw new RuntimeException("Invalid quantity");
        }

        MealFood currentMealFood = mealFoodRepository
                .findByIdAndMeal_Id(mealFoodId, mealId)
                .orElseThrow(() -> new ResourceNotFoundException("MealFood not found!"));

        NutritionResult nutrition = nutritionProvider
                .calculateNutritionByFoodId(currentMealFood.getFatSecretFoodId(), quantity);

        currentMealFood.setQuantity(quantity);
        currentMealFood.setCalories(nutrition.calories());
        currentMealFood.setProtein(nutrition.protein());
        currentMealFood.setCarbs(nutrition.carbs());
        currentMealFood.setFat(nutrition.fat());

        mealFoodRepository.save(currentMealFood);

        return new MealFoodResponse(
                currentMealFood.getId(),
                currentMealFood.getFoodName(),
                currentMealFood.getQuantity(),
                currentMealFood.getUnit(),
                currentMealFood.getCalories(),
                currentMealFood.getCarbs(),
                currentMealFood.getProtein(),
                currentMealFood.getFat()
        );
    }
}
