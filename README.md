# Calorie Tracker API

A backend REST API for tracking meals and nutritional information, built with **Java, Spring Boot, and PostgreSQL**.
The application integrates with the **FatSecret API** to retrieve nutritional data and implements caching and persistence strategies to optimize performance and reduce external API calls.

This project was designed as a **progressively evolving system (V1 → V2 (current) → V3)**, focusing on clean architecture, scalability, and maintainability.

---

# Live API Documentation

Interactive Swagger/OpenAPI documentation is available online:

* **Swagger UI:** [https://calorietracker-api.jircik.dev/swagger-ui/index.html](https://calorietracker-api.jircik.dev/swagger-ui/index.html)
* **OpenAPI spec (JSON):** [https://calorietracker-api.jircik.dev/v3/api-docs](https://calorietracker-api.jircik.dev/v3/api-docs)

---

# Features

### User Management

* Create users
* Configure user profile (age, height, weight, goal weight, daily calorie goal, gender, activity level)
* Retrieve user information
* Retrieve nutrition summaries (daily, weekly, monthly, or custom range)

### Meal Management

* Create meals with a meal type (Breakfast, Lunch, Dinner, Snacks)
* Delete meals
* Retrieve meals by date, grouped by meal type
* Retrieve meal summaries

### Food Tracking

* Add foods to meals
* Remove food from a meal
* Update food quantity with automatic macro recalculation
* Integration with FatSecret food database

### Nutritional Calculations

* Calories
* Carbohydrates
* Protein
* Fat

### Performance Optimizations

* **In-memory caching (Caffeine)**
* **Persistent food cache in database**
* Avoid unnecessary calls to FatSecret API
* Efficient query design

### External Integration

* **FatSecret API (OAuth2 Client Credentials)**

### Testing

* Integration tests using `MockWebServer` to simulate FatSecret API responses
* Web layer tests using `MockMvc`
* Coverage monitored via **JaCoCo**

---

# Tech Stack

* **Java 21**
* **Spring Boot**
* Spring Web
* Spring Data JPA
* Spring Validation
* PostgreSQL
* WebClient (Reactive HTTP client)
* Caffeine Cache
* Maven
* JUnit / MockMvc

---

# Architecture

The project follows a layered architecture:

```
Controller
    ↓
Service
    ↓
Repository
    ↓
Database
```

Key principles used:

* Separation of concerns
* DTO-based API responses
* External API abstraction
* Repository pattern
* Clean service logic

---

# Database Model

Main entities:

```
User
 └── Meal (type: BREAKFAST | LUNCH | DINNER | SNACKS)
       └── MealFood
```

Additionally, the system stores external nutrition data to reduce API calls:

```
Food              → maps food name to FatSecret food ID
FoodNutrition     → stores per-100g macros by FatSecret food ID
```

---

# API Endpoints

## Users

Create user

```
POST /users
```

Configure user profile

```
PATCH /users/{userId}/profile
```

Get user

```
GET /users/{id}
```

Get all users

```
GET /users (Only in development)
```

Get nutrition summary

```
GET /users/{userId}/summary?startDate=YYYY-MM-DD&periodType=DAILY|WEEKLY|MONTHLY|CUSTOM
```

Get nutrition summary (legacy, deprecated)

```
GET /users/{id}/daily-summary?date=YYYY-MM-DD
```

Get meals by date

```
GET /users/{userId}/meals?date=YYYY-MM-DD
```

---

## Meals

Create meal

```
POST /meals
```

Delete meal

```
DELETE /meals/{mealId}
```

Get meal summary

```
GET /meals/{mealId}/summary
```

---

## Meal Foods

Add food to meal

```
POST /meals/{mealId}/foods
```

Update food quantity

```
PATCH /meals/{mealId}/foods/{mealFoodId}
```

Delete food from meal

```
DELETE /meals/{mealId}/foods/{mealFoodId}
```

---

# Example Response

Example response for retrieving meals by date:

```json
{
  "userId": 1,
  "date": "2026-04-08",
  "meals": {
    "BREAKFAST": {
      "mealId": 1,
      "dateTime": "2026-04-08T08:00:00",
      "mealType": "BREAKFAST",
      "foods": [
        {
          "id": 1,
          "foodName": "oats",
          "quantity": 80.0,
          "unit": "g",
          "calories": 303.2,
          "carbs": 54.61,
          "protein": 10.54,
          "fat": 5.18
        }
      ]
    },
    "LUNCH": {
      "mealId": 2,
      "dateTime": "2026-04-08T12:30:00",
      "mealType": "LUNCH",
      "foods": [...]
    },
    "DINNER": null,
    "SNACKS": null
  }
}
```

---

# External API Integration

The application integrates with the **FatSecret API** to retrieve nutritional information.

Flow:

```
Food name
   ↓
Search FatSecret API
   ↓
Retrieve Food ID
   ↓
Fetch nutritional details
   ↓
Calculate nutrition for requested quantity
```

Caching layers prevent redundant external requests.

---

# Caching Strategy

Two levels of caching are implemented:

### 1. In-Memory Cache (Caffeine)

Used for quick access to frequently requested foods.

### 2. Persistent Cache (Database)

Stores previously retrieved nutritional data.

Benefits:

* Reduces external API calls
* Improves performance
* Allows reuse of known foods

---

# Quality & Tests

The project has an automated test suite focused on high reliability and coverage.

* **Integration Tests:** Using `MockWebServer` to simulate real responses (happy paths and 4xx/5xx errors) from the FatSecret API.

* **Web Tests (Slice):** Using `MockMvc` to validate REST contracts, JSON mapping, and HTTP status codes.

* **Coverage:** Monitored via **JaCoCo**, reaching **97% statement coverage** and **82% branch coverage**.

---

# Roadmap

## V2 (Current)

* Users with configurable profile
* Meals with meal type (Breakfast, Lunch, Dinner, Snacks)
* Food tracking with automatic macro calculation
* FatSecret API integration
* Two-level caching (in-memory + persistent)
* Daily, weekly, monthly, and custom period summaries
* Full CRUD for meals and meal foods
* Automated tests

---

## V3 (Planned)

* Frontend application
* JWT authentication
* User login
* Full client integration

---

# Author

Arthur Jircik Cronemberger

Software Engineering student and backend developer focused on building scalable and well-structured systems.
