package com.jircik.calorietrackerapi.integration.fatsecret;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.jircik.calorietrackerapi.exception.IntegrationException;
import com.jircik.calorietrackerapi.integration.dto.FoodDetailsResponse;
import com.jircik.calorietrackerapi.integration.dto.FoodSearchResponse;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.*;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

@DisplayName("FatSecretFoodClient")
class FatSecretFoodClientTest {

    private MockWebServer mockWebServer;
    private FatSecretFoodClient foodClient;

    private Cache<String, FoodDetailsResponse> foodDetailsCache;
    private Cache<String, List<FoodSearchResponse.Food>> foodSearchCache;

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        WebClient webClient = WebClient.builder()
                .baseUrl(mockWebServer.url("/").toString())
                .build();

        foodDetailsCache = Caffeine.newBuilder().build();
        foodSearchCache = Caffeine.newBuilder().build();

        foodClient = new FatSecretFoodClient(webClient, foodDetailsCache, foodSearchCache);
    }

    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    // searchFoods
    @Nested
    @DisplayName("searchFoods")
    class SearchFoods {

        private static final String VALID_SEARCH_JSON = """
                {
                    "foods": {
                        "food": [
                            {
                                "food_id": "123",
                                "food_name": "Arroz Branco",
                                "brand_name": null,
                                "food_description": "Per 100g - Calories: 130",
                                "food_type": "Generic",
                                "food_url": null
                            },
                            {
                                "food_id": "456",
                                "food_name": "Arroz Integral",
                                "brand_name": null,
                                "food_description": "Per 100g - Calories: 111",
                                "food_type": "Generic",
                                "food_url": null
                            }
                        ],
                        "max_results": "5",
                        "page_number": "0",
                        "total_results": "2"
                    }
                }
                """;

        @Test
        @DisplayName("deve retornar lista de alimentos com resposta válida")
        void shouldReturnFoodListOnValidResponse() throws InterruptedException {
            mockWebServer.enqueue(new MockResponse()
                    .setBody(VALID_SEARCH_JSON)
                    .addHeader("Content-Type", "application/json"));

            List<FoodSearchResponse.Food> result = foodClient.searchFoods("Arroz");

            assertThat(result).hasSize(2);
            assertThat(result.get(0).food_id()).isEqualTo("123");
            assertThat(result.get(0).food_name()).isEqualTo("Arroz Branco");

            RecordedRequest request = mockWebServer.takeRequest();
            assertThat(request.getPath()).contains("/search");
            assertThat(request.getPath()).contains("q=Arroz");
        }

        @Test
        @DisplayName("deve retornar lista do cache na segunda chamada")
        void shouldReturnCachedListOnSecondCall() {
            mockWebServer.enqueue(new MockResponse()
                    .setBody(VALID_SEARCH_JSON)
                    .addHeader("Content-Type", "application/json"));

            List<FoodSearchResponse.Food> first = foodClient.searchFoods("Arroz");
            List<FoodSearchResponse.Food> second = foodClient.searchFoods("arroz");

            assertThat(first.get(0).food_id()).isEqualTo("123");
            assertThat(second.get(0).food_id()).isEqualTo("123");
            assertThat(mockWebServer.getRequestCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("deve lançar IntegrationException quando lista de alimentos está vazia")
        void shouldThrowOnEmptyFoodList() {
            String emptyJson = """
                    {
                        "foods": {
                            "food": [],
                            "max_results": "5",
                            "page_number": "0",
                            "total_results": "0"
                        }
                    }
                    """;

            mockWebServer.enqueue(new MockResponse()
                    .setBody(emptyJson)
                    .addHeader("Content-Type", "application/json"));

            assertThatThrownBy(() -> foodClient.searchFoods("xyznonexistent"))
                    .isInstanceOf(IntegrationException.class)
                    .hasMessageContaining("No results found for");
        }

        @Test
        @DisplayName("deve lançar IntegrationException quando foods é nulo")
        void shouldThrowOnNullFoods() {
            mockWebServer.enqueue(new MockResponse()
                    .setBody("{}")
                    .addHeader("Content-Type", "application/json"));

            assertThatThrownBy(() -> foodClient.searchFoods("anything"))
                    .isInstanceOf(IntegrationException.class)
                    .hasMessageContaining("No results found for");
        }

        @Test
        @DisplayName("deve lançar IntegrationException quando food list é nula")
        void shouldThrowOnNullFoodList() {
            String nullFoodListJson = """
                    {
                        "foods": {
                            "max_results": "5",
                            "page_number": "0",
                            "total_results": "0"
                        }
                    }
                    """;

            mockWebServer.enqueue(new MockResponse()
                    .setBody(nullFoodListJson)
                    .addHeader("Content-Type", "application/json"));

            assertThatThrownBy(() -> foodClient.searchFoods("anything"))
                    .isInstanceOf(IntegrationException.class)
                    .hasMessageContaining("No results found for");
        }

        @Test
        @DisplayName("deve lançar IntegrationException em erro 4xx do cliente")
        void shouldThrowOn4xxClientError() {
            mockWebServer.enqueue(new MockResponse()
                    .setResponseCode(404)
                    .setBody("Not Found")
                    .addHeader("Content-Type", "text/plain"));

            assertThatThrownBy(() -> foodClient.searchFoods("Arroz"))
                    .isInstanceOf(IntegrationException.class)
                    .hasMessageContaining("FatSecret client error");
        }

        @Test
        @DisplayName("deve lançar IntegrationException em erro 5xx do servidor")
        void shouldThrowOn5xxServerError() {
            mockWebServer.enqueue(new MockResponse()
                    .setResponseCode(500)
                    .setBody("Internal Server Error")
                    .addHeader("Content-Type", "text/plain"));

            assertThatThrownBy(() -> foodClient.searchFoods("Arroz"))
                    .isInstanceOf(IntegrationException.class)
                    .hasMessageContaining("FatSecret server error");
        }
    }

    // getFoodById
    @Nested
    @DisplayName("getFoodById")
    class GetFoodById {

        private static final String VALID_DETAILS_JSON = """
                {
                    "food": {
                        "food_id": "456",
                        "food_name": "Arroz Integral",
                        "brand_name": null,
                        "servings": {
                            "serving": [
                                {
                                    "serving_id": "1",
                                    "serving_description": "Per 100g",
                                    "metric_serving_amount": "100.00",
                                    "metric_serving_unit": "g",
                                    "calories": "111.00",
                                    "carbohydrate": "23.00",
                                    "protein": "2.60",
                                    "fat": "0.90",
                                    "is_default": "1"
                                }
                            ]
                        }
                    }
                }
                """;

        @Test
        @DisplayName("deve retornar detalhes do alimento com resposta válida")
        void shouldReturnFoodDetailsOnValidResponse() throws InterruptedException {
            mockWebServer.enqueue(new MockResponse()
                    .setBody(VALID_DETAILS_JSON)
                    .addHeader("Content-Type", "application/json"));

            FoodDetailsResponse result = foodClient.getFoodById("456");

            assertThat(result).isNotNull();
            assertThat(result.food()).isNotNull();
            assertThat(result.food().food_id()).isEqualTo("456");
            assertThat(result.food().food_name()).isEqualTo("Arroz Integral");
            assertThat(result.food().servings().serving()).hasSize(1);

            RecordedRequest request = mockWebServer.takeRequest();
            assertThat(request.getPath()).startsWith("/food/456");
        }

        @Test
        @DisplayName("deve retornar detalhes do cache na segunda chamada")
        void shouldReturnCachedDetailsOnSecondCall() {
            mockWebServer.enqueue(new MockResponse()
                    .setBody(VALID_DETAILS_JSON)
                    .addHeader("Content-Type", "application/json"));

            FoodDetailsResponse first = foodClient.getFoodById("456");
            FoodDetailsResponse second = foodClient.getFoodById("456");

            assertThat(first.food().food_id()).isEqualTo("456");
            assertThat(second.food().food_id()).isEqualTo("456");
            assertThat(mockWebServer.getRequestCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("deve lançar IntegrationException quando food é nulo na resposta")
        void shouldThrowOnNullFoodInResponse() {
            mockWebServer.enqueue(new MockResponse()
                    .setBody("{}")
                    .addHeader("Content-Type", "application/json"));

            assertThatThrownBy(() -> foodClient.getFoodById("999"))
                    .isInstanceOf(IntegrationException.class)
                    .hasMessageContaining("Invalid response from FatSecret (getFoodById)");
        }

        @Test
        @DisplayName("deve lançar IntegrationException em erro 4xx do cliente")
        void shouldThrowOn4xxClientError() {
            mockWebServer.enqueue(new MockResponse()
                    .setResponseCode(404)
                    .setBody("Not Found")
                    .addHeader("Content-Type", "text/plain"));

            assertThatThrownBy(() -> foodClient.getFoodById("456"))
                    .isInstanceOf(IntegrationException.class)
                    .hasMessageContaining("FatSecret client error (getFoodById)");
        }

        @Test
        @DisplayName("deve lançar IntegrationException em erro 5xx do servidor")
        void shouldThrowOn5xxServerError() {
            mockWebServer.enqueue(new MockResponse()
                    .setResponseCode(500)
                    .setBody("Internal Server Error")
                    .addHeader("Content-Type", "text/plain"));

            assertThatThrownBy(() -> foodClient.getFoodById("456"))
                    .isInstanceOf(IntegrationException.class)
                    .hasMessageContaining("FatSecret server error (getFoodById)");
        }
    }
}