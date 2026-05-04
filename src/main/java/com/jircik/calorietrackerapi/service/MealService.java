package com.jircik.calorietrackerapi.service;

import com.jircik.calorietrackerapi.domain.dto.request.AddFoodToMealRequest;
import com.jircik.calorietrackerapi.domain.dto.response.MealFoodResponse;
import com.jircik.calorietrackerapi.domain.dto.response.MealResponse;
import com.jircik.calorietrackerapi.domain.dto.response.MealSummaryResponse;
import com.jircik.calorietrackerapi.domain.dto.response.MealWithFoodsResponse;
import com.jircik.calorietrackerapi.domain.entity.Meal;
import com.jircik.calorietrackerapi.domain.entity.MealFood;
import com.jircik.calorietrackerapi.domain.entity.MealTypeEnum;
import com.jircik.calorietrackerapi.domain.entity.User;
import com.jircik.calorietrackerapi.domain.fatsecret.NutritionProvider;
import com.jircik.calorietrackerapi.domain.fatsecret.NutritionResult;
import com.jircik.calorietrackerapi.exception.DuplicateMealException;
import com.jircik.calorietrackerapi.exception.ResourceNotFoundException;
import com.jircik.calorietrackerapi.repository.MealFoodRepository;
import com.jircik.calorietrackerapi.repository.MealRepository;
import com.jircik.calorietrackerapi.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

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
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        findExistingMealOnDate(userId, mealType, date).ifPresent(existing -> {
            throw new DuplicateMealException(
                    "A " + mealType.name().toLowerCase() + " already exists for this day",
                    existing.getId());
        });

        Meal meal = new Meal();
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

    public MealResponse updateMeal(Long mealId, LocalDateTime newDateTime, Long callerUserId) {
        Meal meal = mealRepository.findById(mealId)
                .orElseThrow(() -> new ResourceNotFoundException("Meal not found"));
        verifyMealOwnership(meal, callerUserId);

        if (!meal.getDatetime().toLocalDate().equals(newDateTime.toLocalDate())) {
            findExistingMealOnDate(callerUserId, meal.getMealType(), newDateTime)
                    .filter(other -> !other.getId().equals(mealId))
                    .ifPresent(other -> {
                        throw new DuplicateMealException(
                                "A " + meal.getMealType().name().toLowerCase()
                                        + " already exists on the target day",
                                other.getId());
                    });
        }

        meal.setDatetime(newDateTime);
        Meal saved = mealRepository.save(meal);

        return new MealResponse(
                saved.getId(),
                saved.getUser().getId(),
                saved.getDatetime(),
                saved.getMealType(),
                saved.getCreatedAt()
        );
    }

    private Optional<Meal> findExistingMealOnDate(Long userId, MealTypeEnum mealType, LocalDateTime dateTime) {
        LocalDateTime start = dateTime.toLocalDate().atStartOfDay();
        LocalDateTime end = dateTime.toLocalDate().atTime(LocalTime.MAX);
        return mealRepository.findByUser_IdAndMealTypeAndDatetimeBetween(userId, mealType, start, end);
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

        return new MealSummaryResponse(
                mealId,
                totalCalories,
                totalProtein,
                totalCarbs,
                totalFat,
                foods.size()
        );
    }

    public MealWithFoodsResponse getMealWithFoods(Long mealId, Long callerUserId) {
        Meal meal = mealRepository.findById(mealId)
                .orElseThrow(() -> new ResourceNotFoundException("Meal not found"));
        verifyMealOwnership(meal, callerUserId);

        List<MealFood> foods = mealFoodRepository.findByMeal_Id(mealId);
        List<MealFoodResponse> foodResponses = foods.stream()
                .map(f -> new MealFoodResponse(
                        f.getId(), f.getFoodName(), f.getQuantity(), f.getUnit(),
                        f.getCalories(), f.getCarbs(), f.getProtein(), f.getFat()))
                .toList();

        double totalCalories = foods.stream().mapToDouble(MealFood::getCalories).sum();
        double totalProtein  = foods.stream().mapToDouble(MealFood::getProtein).sum();
        double totalCarbs    = foods.stream().mapToDouble(MealFood::getCarbs).sum();
        double totalFat      = foods.stream().mapToDouble(MealFood::getFat).sum();

        return new MealWithFoodsResponse(
                meal.getId(), meal.getDatetime(), meal.getMealType(),
                foodResponses, totalCalories, totalProtein, totalCarbs, totalFat);
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
            throw new IllegalArgumentException("Quantity must be greater than zero");
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
