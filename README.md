# CalorieTracker — Backend API

REST API for **CalorieTracker**, a nutrition-tracking app: log meals from
the FatSecret food database, track macros and water, view daily / weekly /
monthly summaries.

Built with **Java 21, Spring Boot 3.4, PostgreSQL**, and **Spring Security
(JWT)**. Integrates with the FatSecret food database through a Cloudflare
Worker proxy that handles OAuth2 token rotation. Caches nutrition data in
both memory (Caffeine) and the database to minimise external API calls.

---

## Live

* **Web app:** [https://calorietracker.jircik.dev](https://calorietracker.jircik.dev)
* **Frontend repo:** [CalorieTracker-Frontend](https://github.com/jircik/CalorieTracker-Frontend)
* **API host:** `https://calorietracker-api.jircik.dev` (used by the web app; Swagger disabled in prod, browse it locally at `/swagger-ui/index.html` when running with `./mvnw spring-boot:run`)

Deployed on **Railway** (Spring Boot + Postgres plugin). The FatSecret
OAuth2 proxy lives as a Cloudflare Worker (`cloudflare-worker/`).

---

## Features

### Authentication
* Email / password registration with **BCrypt** hashing
* JWT login (24h expiry); the token encodes `userId` and `email`
* `401` on invalid token, with the frontend automatically redirecting
* `403` on cross-user access attempts (defense in depth)

### Users
* Configurable profile: age, height (cm), current weight, weight goal,
  daily calorie goal, daily water goal, gender, activity level
* Partial updates (`PATCH`) — only non-null fields are applied
* Nutrition summaries with flexible periods: `DAILY`, `WEEKLY`, `MONTHLY`,
  `CUSTOM`

### Meals
* Create, read, update, delete meals — one of `BREAKFAST`, `LUNCH`,
  `DINNER`, `SNACKS`
* **One meal per type per day**: a second meal of the same type on the
  same calendar day returns **`409 Conflict`** with the existing meal id
  in the body, so the client can redirect rather than error
* `PATCH /meals/{id}` updates a meal's `dateTime`, with the same
  duplicate guard when the change moves the meal to a different day
* Get all of a user's meals for a given date, grouped by meal type

### Foods (FatSecret-backed)
* Search foods via FatSecret
* Add food to a meal — server fetches nutrition, scales it to the chosen
  quantity, and stores the macros on `MealFood` so historical data is
  immutable even if FatSecret updates the source
* Update food quantity — recalculates macros automatically
* Remove food from a meal

### Water tracking
* Log water intake in millilitres
* Daily totals against the user's `dailyWaterGoalMl`
* List, add, and delete log entries per day

### Performance
* **Caffeine** in-memory cache for frequently accessed nutrition data
* **Persistent cache** in `food_nutrition` table — once a FatSecret food
  has been resolved, future requests skip the external call entirely
* Indexed reads for the date-grouped meal queries on the dashboard / diary

### Quality
* **152 automated tests**, JaCoCo coverage **~97% statement / 82% branch**
* Web slice tests (`@WebMvcTest` + `MockMvc`) verify REST contracts,
  validation, and JSON shapes
* Service tests use Mockito; integration tests against the FatSecret
  proxy use `MockWebServer` to assert correct request shaping and 4xx/5xx
  handling

---

## Tech stack

* **Java 21**, Spring Boot 3.4
* Spring Web, Spring Security, Spring Data JPA, Spring Validation
* JJWT (token issue + verification)
* PostgreSQL (prod, via Railway), H2 (tests)
* Spring `WebClient` (reactive HTTP client for the FatSecret worker)
* Caffeine (in-memory cache)
* Maven, JUnit 5, Mockito, MockWebServer, JaCoCo

---

## Architecture

Layered Spring application:

```
Controller  →  Service  →  Repository  →  Database
                  ↓
            FatSecret proxy (Cloudflare Worker, OAuth2 + KV token cache)
```

Conventions:

* DTOs in `domain/dto/{request, response}` — entities are never exposed
* `@RestControllerAdvice` (`GlobalExceptionHandler`) maps domain
  exceptions (`ResourceNotFoundException`, `DuplicateMealException`,
  validation, bad credentials, integration failures) to consistent
  `{ status, message, path, timestamp }` error bodies
* `LocalDateTime` serialises as `"2026-04-26T12:30:00"` (no `Z`),
  `LocalDate` as `"2026-04-26"`
* Schema is JPA-managed (`spring.jpa.hibernate.ddl-auto=update`); for
  destructive entity changes during dev we drop the affected tables

---

## Database model

Five tables:

| Table | Purpose |
|---|---|
| `users` | Account credentials + profile (age, height, weight, goals, gender, activity level) |
| `meals` | A meal slot for a user on a given `datetime` with a `mealType` (BREAKFAST / LUNCH / DINNER / SNACKS) |
| `meal_foods` | Foods logged against a meal, with **denormalised macros** snapshotted at log time |
| `water_logs` | Per-user water intake entries (`amountMl`, `loggedAt`) |
| `food_nutrition` | Cache of per-100g macros keyed by FatSecret food id |

Relationships:

```
users  1 ──┬── *  meals  1 ── *  meal_foods
           └── *  water_logs

food_nutrition   (independent cache, joined logically by fatSecretFoodId on meal_foods)
```

`meal_foods` stores the macros at the time of logging — historical data
stays stable even if FatSecret edits the source food. `food_nutrition`
is a pure read-through cache: if a food id isn't in it yet, the service
fetches from FatSecret and inserts; subsequent lookups skip the network.

---

## API surface

A short overview; for full schemas, run the app locally and browse the Swagger UI at `http://localhost:8080/swagger-ui/index.html`.

### Auth
| Method | Path | Notes |
|---|---|---|
| `POST` | `/auth/register` | returns `{ token, userId, name, email }` |
| `POST` | `/auth/login` | returns the same shape |

### Users
| Method | Path |
|---|---|
| `GET` | `/users/{id}` |
| `PATCH` | `/users/{userId}/profile` |
| `GET` | `/users/{userId}/meals?date=YYYY-MM-DD` |
| `GET` | `/users/{userId}/summary?startDate=YYYY-MM-DD&[endDate=YYYY-MM-DD&]periodType=DAILY\|WEEKLY\|MONTHLY\|CUSTOM` |
| `GET` | `/users/{userId}/water?date=YYYY-MM-DD` |
| `POST` | `/users/{userId}/water` |
| `DELETE` | `/users/{userId}/water/{logId}` |

### Meals
| Method | Path | Notes |
|---|---|---|
| `POST` | `/meals` | `409` with `existingMealId` if duplicate (same user/type/day) |
| `GET` | `/meals/{mealId}` | meal + foods + macro totals |
| `GET` | `/meals/{mealId}/summary` | macro totals only |
| `PATCH` | `/meals/{mealId}` | updates `dateTime`, same `409` rule when moving to a new day |
| `DELETE` | `/meals/{mealId}` | cascade-deletes foods |

### Meal foods
| Method | Path |
|---|---|
| `POST` | `/meals/{mealId}/foods` |
| `PATCH` | `/meals/{mealId}/foods/{mealFoodId}` |
| `DELETE` | `/meals/{mealId}/foods/{mealFoodId}` |

### Foods
| Method | Path |
|---|---|
| `GET` | `/foods/search?query=...` |

All endpoints except `/auth/*` and Swagger require
`Authorization: Bearer <jwt>`.

### Example: meals by date

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
      ],
      "totalCalories": 303.2,
      "totalProtein": 10.54,
      "totalCarbs": 54.61,
      "totalFat": 5.18
    },
    "LUNCH": { "mealId": 2, "dateTime": "2026-04-08T12:30:00", "mealType": "LUNCH", "foods": [], "totalCalories": 0.0, "totalProtein": 0.0, "totalCarbs": 0.0, "totalFat": 0.0 },
    "DINNER": null,
    "SNACKS": null
  }
}
```

### Example: duplicate meal (`409`)

```json
{
  "status": 409,
  "message": "A lunch already exists for this day",
  "existingMealId": 42,
  "path": "/meals",
  "timestamp": "2026-05-04T12:00:00"
}
```

---

## Running locally

Requirements: Java 21, Maven (the wrapper is included), and a Postgres
instance (or Docker).

```bash
cd backend
./mvnw spring-boot:run        # http://localhost:8080
./mvnw test                   # full suite (152 tests)
```

Required environment variables (`application.yml` reads them):

| Variable | Purpose |
|---|---|
| `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD`, `DB_SSL_MODE` | Postgres connection |
| `JWT_SECRET` | symmetric key used to sign JWTs |
| `FATSECRET_WORKER_URL` | base URL of the Cloudflare Worker proxy |
| `FATSECRET_PROXY_KEY` | shared secret sent as `X-Proxy-Key` |
| `CORS_ALLOWED_ORIGINS` | comma-separated origins permitted by the API |
| `SPRING_PROFILES_ACTIVE` | set to `prod` in production (silences SQL logs) |

Sample HTTP requests live in [`requests.http`](./requests.http) and
[`dev.http`](./dev.http) — the latter is an end-to-end flow that exercises
the meal duplicate guard and `PATCH /meals/{id}`.

---

## FatSecret integration

```
Search query
   ↓
Cloudflare Worker (X-Proxy-Key, OAuth2 token in KV, refresh on expiry)
   ↓
FatSecret API
   ↓
Worker returns JSON
   ↓
Backend caches nutrition in food_nutrition + Caffeine
```

The Worker absorbs OAuth2 complexity so the backend only needs a
single shared secret. Token refresh is handled out-of-band in KV.

---

## Roadmap

The frontend has shipped (V3 is live), features that are yet to come are:

* **Recent / favorite foods** with one-tap quick-add (next up)
* **Auto-calculate** calorie + water goals from profile fields (Mifflin-St Jeor TDEE)
* **Custom foods** for items FatSecret doesn't have
* **Streaks** and **weight tracking time series**
* **Barcode scanning** 
* **PWA install + offline read**, **Web Push** reminders
* **Quality:** FatSecret search caching in KV, Sentry, Playwright E2E

---

## Author

**Arthur Jircik Cronemberger** — software engineering student and
full-stack developer focused on building scalable, well-structured systems.
