package com.jircik.calorietrackerapi.service;

import com.jircik.calorietrackerapi.domain.dto.request.ConfigureUserProfileRequest;
import com.jircik.calorietrackerapi.domain.dto.request.GetSummaryRequest;
import com.jircik.calorietrackerapi.domain.dto.response.*;
import com.jircik.calorietrackerapi.domain.entity.Meal;
import com.jircik.calorietrackerapi.domain.entity.MealFood;
import com.jircik.calorietrackerapi.domain.entity.MealTypeEnum;
import com.jircik.calorietrackerapi.domain.entity.User;
import com.jircik.calorietrackerapi.exception.ResourceNotFoundException;
import com.jircik.calorietrackerapi.repository.MealFoodRepository;
import com.jircik.calorietrackerapi.repository.MealRepository;
import com.jircik.calorietrackerapi.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final MealRepository mealRepository;
    private final MealFoodRepository mealFoodRepository;

    public UserResponse configureUserProfile(ConfigureUserProfileRequest request, Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (request.age() != null) user.setAge(request.age());
        if (request.heightInMeters() != null) user.setHeightInMeters(request.heightInMeters());
        if (request.currentWeight() != null) user.setCurrentWeight(request.currentWeight());
        if (request.weightGoal() != null) user.setWeightGoal(request.weightGoal());
        if (request.dailyCalorieIntakeGoal() != null) user.setDailyCalorieIntakeGoal(request.dailyCalorieIntakeGoal());
        if (request.dailyWaterGoalMl() != null) user.setDailyWaterGoalMl(request.dailyWaterGoalMl());
        if (request.gender() != null) user.setGender(request.gender());
        if (request.activityLevel() != null) user.setActivityLevel(request.activityLevel());

        userRepository.save(user);

        return new UserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getAge(),
                user.getHeightInMeters(),
                user.getCurrentWeight(),
                user.getWeightGoal(),
                user.getDailyCalorieIntakeGoal(),
                user.getDailyWaterGoalMl(),
                user.getGender(),
                user.getActivityLevel()
        );
    }

    public UserResponse getUser(Long id) {

        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        return new UserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getAge(),
                user.getHeightInMeters(),
                user.getCurrentWeight(),
                user.getWeightGoal(),
                user.getDailyCalorieIntakeGoal(),
                user.getDailyWaterGoalMl(),
                user.getGender(),
                user.getActivityLevel()
        );
    }

    //metodo somente em dev, pois user não pode ver outros users
    public List<UserResponse> getAllUsers() {
        return userRepository.findAll()
                .stream()
                .map(user -> new UserResponse(
                        user.getId(),
                        user.getName(),
                        user.getEmail(),
                        user.getAge(),
                        user.getHeightInMeters(),
                        user.getCurrentWeight(),
                        user.getWeightGoal(),
                        user.getDailyCalorieIntakeGoal(),
                        user.getDailyWaterGoalMl(),
                        user.getGender(),
                        user.getActivityLevel()
                ))
                .toList();
    }

    public MealsByDateResponse getMealsByDate(Long userId, LocalDate date) {

        userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.atTime(LocalTime.MAX);

        List<MealFood> foods = mealFoodRepository
                .findByMeal_User_IdAndMeal_DatetimeBetween(userId, start, end);

        Map<MealTypeEnum, List<MealFood>> foodsByType =
                foods.stream().collect(Collectors.groupingBy(food -> food.getMeal().getMealType()));

        Map<MealTypeEnum, MealWithFoodsResponse> mealsMap = new LinkedHashMap<>();
        for (MealTypeEnum type : MealTypeEnum.values()) {
            List<MealFood> typeFoods = foodsByType.get(type);
            if (typeFoods == null) {
                mealsMap.put(type, null);
            } else {
                Meal meal = typeFoods.getFirst().getMeal();
                List<MealFoodResponse> foodResponses = typeFoods.stream()
                        .map(food -> new MealFoodResponse(
                                food.getId(),
                                food.getFoodName(),
                                food.getQuantity(),
                                food.getUnit(),
                                food.getCalories(),
                                food.getCarbs(),
                                food.getProtein(),
                                food.getFat()
                        ))
                        .toList();
                mealsMap.put(type, new MealWithFoodsResponse(
                        meal.getId(),
                        meal.getDatetime(),
                        meal.getMealType(),
                        foodResponses
                ));
            }
        }

        return new MealsByDateResponse(userId, date, mealsMap);
    }

    public DailySummaryResponse getDailySummary (Long userId, LocalDate date) {

        userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("user not found!"));

        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.atTime(LocalTime.MAX);

        List<Meal> meals =
                mealRepository.findByUser_IdAndDatetimeBetweenOrderByDatetimeAsc(userId, start, end);

        List<Long> mealIds = meals.stream().map(Meal::getId).toList();

        List<MealFood> foods = mealFoodRepository.findByMeal_IdInOrderByCreatedAtAsc(mealIds);

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

        long mealCount = meals.size();
        long foodCount = foods.size();


        return new DailySummaryResponse(
                userId,
                date,
                totalCalories,
                totalProtein,
                totalCarbs,
                totalFat,
                mealCount,
                foodCount
        );
    }

    public SummaryResponse getPeriodSummary(GetSummaryRequest request) {

        if (request.userId() == null || request.startDate() == null) {
            throw new IllegalArgumentException("userId e startDate são obrigatórios");
        }

        boolean requiresEndDate = request.periodType() == null ||
                !Set.of("DAILY", "WEEKLY", "MONTHLY").contains(request.periodType().toUpperCase());

        if (requiresEndDate && request.endDate() == null) {
            throw new IllegalArgumentException(
                    "endDate é obrigatório quando periodType não é WEEKLY ou MONTHLY");
        }

        if (request.endDate() != null && request.startDate().isAfter(request.endDate())) {
            throw new IllegalArgumentException("startDate não pode ser depois de endDate");
        }

        LocalDate finalStart = request.startDate();
        LocalDate finalEnd   = request.endDate();

        String effectivePeriodType = request.periodType() != null
                ? request.periodType().toUpperCase()
                : "CUSTOM";

        if (request.periodType() != null) {
            switch (effectivePeriodType) {
                case "DAILY" -> finalEnd = finalStart;
                case "WEEKLY" -> {
                    finalStart = request.startDate().with(DayOfWeek.MONDAY);
                    finalEnd   = finalStart.plusDays(6);
                }
                case "MONTHLY" -> {
                    finalStart = request.startDate().withDayOfMonth(1);
                    finalEnd   = request.startDate().withDayOfMonth(request.startDate().lengthOfMonth());
                }
                case "CUSTOM" -> {}
                default -> throw new IllegalArgumentException("periodType inválido: " + request.periodType());
            }
        }

        if (finalEnd == null) {
            throw new IllegalStateException("Intervalo final não determinado corretamente");
        }

        long daysBetween = ChronoUnit.DAYS.between(finalStart, finalEnd);
        if (daysBetween > 366) {
            throw new IllegalArgumentException("O período máximo permitido é 366 dias");
        }

        LocalDateTime from = finalStart.atStartOfDay();
        LocalDateTime to   = finalEnd.plusDays(1).atStartOfDay().minusNanos(1);

        User user = userRepository.findById(request.userId())
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));

        List<Meal> meals = mealRepository.findByUser_IdAndDatetimeBetweenOrderByDatetimeAsc(
                user.getId(), from, to);

        int daysInPeriod = (int) daysBetween + 1;

        if (meals.isEmpty()) {
            return new SummaryResponse(
                    user.getId(),
                    finalStart,
                    finalEnd,
                    effectivePeriodType,
                    0.0, 0.0, 0.0, 0.0,
                    0L, 0L,
                    daysInPeriod,
                    0.0
            );
        }

        List<Long> mealIds = meals.stream().map(Meal::getId).toList();

        List<MealFood> foods = mealFoodRepository.findByMeal_IdInOrderByCreatedAtAsc(mealIds);

        double totalCalories = foods.stream().mapToDouble(MealFood::getCalories).sum();
        double totalProtein  = foods.stream().mapToDouble(MealFood::getProtein).sum();
        double totalCarbs    = foods.stream().mapToDouble(MealFood::getCarbs).sum();
        double totalFat      = foods.stream().mapToDouble(MealFood::getFat).sum();

        long mealCount = meals.size();
        long foodCount = foods.size();

        double avgCalories = daysInPeriod > 0 ? totalCalories / daysInPeriod : 0.0;

        return new SummaryResponse(
                user.getId(),
                finalStart,
                finalEnd,
                effectivePeriodType,
                totalCalories,
                totalProtein,
                totalCarbs,
                totalFat,
                mealCount,
                foodCount,
                daysInPeriod,
                avgCalories
        );
    }

}
