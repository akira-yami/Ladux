package org.akira.ladux.catalog.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.akira.ladux.AbstractIntegrationTest;
import org.akira.ladux.catalog.entity.ProductVariant;
import org.akira.ladux.catalog.repository.ProductVariantRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Public HTTP contract and live catalog safety through the real security/validation chains. */
class DiscoveryHttpIntegrationTest extends AbstractIntegrationTest {
    @LocalServerPort private int port;
    @Autowired private ProductVariantRepository variants;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

    @Test
    void anonymousDiscoverySupportsFocusAndQueryAndRejectsAnOversizedQuery() throws Exception {
        HttpResponse<String> focus = get("/api/v1/discovery/suggestions");
        assertEquals(200, focus.statusCode(), focus.body());
        JsonNode focusBody = objectMapper.readTree(focus.body());
        assertTrue(focusBody.path("products").isArray(), focus.body());
        assertTrue(focusBody.path("products").size() > 0, "Seed catalog must provide public focus suggestions");
        assertTrue(Set.of("BEST_SELLER", "FEATURED").contains(focusBody.path("mode").asText()));

        HttpResponse<String> search = get("/api/v1/discovery/suggestions?q=asus");
        assertEquals(200, search.statusCode(), search.body());
        JsonNode searchBody = objectMapper.readTree(search.body());
        assertEquals("SEARCH", searchBody.path("mode").asText());
        assertTrue(searchBody.path("products").isArray(), search.body());
        assertTrue(searchBody.path("products").size() > 0, "Seed ASUS products must be discoverable");

        HttpResponse<String> oversized = get("/api/v1/discovery/suggestions?q=" + "a".repeat(121));
        assertEquals(400, oversized.statusCode(), oversized.body());
    }

    @Test
    void anonymousSmartFitReturnsThreeDistinctProductsAtTheirCurrentSellableSkuPrices() throws Exception {
        BigDecimal budget = new BigDecimal("100000000");
        HttpResponse<String> response = postSmartFit("""
                {"budgetMax":100000000,"purpose":"PROGRAMMING","priority":"PERFORMANCE","limit":3}
                """);
        assertEquals(200, response.statusCode(), response.body());
        JsonNode body = objectMapper.readTree(response.body());
        // Success is a direct DTO, so clients can read recommendations without a data wrapper.
        JsonNode recommendations = body.path("recommendations");
        assertTrue(recommendations.isArray(), response.body());
        assertEquals(3, recommendations.size(), response.body());
        assertEquals(50, body.path("weights").path("purpose").asInt());
        assertFalse(body.path("disclaimer").asText().isBlank());
        Set<Integer> productIds = new HashSet<>();
        for (JsonNode recommendation : recommendations) {
            int variantId = recommendation.path("variantId").asInt();
            ProductVariant variant = variants.findById(variantId).orElseThrow();
            assertTrue(variant.isActive(), "Recommended SKU must still be active: " + variantId);
            assertTrue(variant.getProduct().isActive(), "Recommended product must be active: " + variantId);
            assertTrue(variant.getStockQuantity() > 0, "Recommended SKU must be in stock: " + variantId);
            assertEquals(variant.getProduct().getId().intValue(), recommendation.path("productId").asInt());
            assertTrue(productIds.add(recommendation.path("productId").asInt()), "Recommendations must contain distinct products");
            BigDecimal databasePrice = variant.getDiscountPrice() == null ? variant.getPrice() : variant.getDiscountPrice();
            BigDecimal responsePrice = recommendation.path("price").decimalValue();
            assertEquals(0, databasePrice.compareTo(responsePrice), "Response price must match the selected SKU");
            assertTrue(responsePrice.signum() > 0 && responsePrice.compareTo(budget) <= 0);
            assertTrue(recommendation.path("score").asDouble() >= 0 && recommendation.path("score").asDouble() <= 100);
            assertTrue(recommendation.path("reasons").isArray());
            assertTrue(recommendation.path("tradeoffs").isArray());
        }
    }

    @Test
    void anonymousSmartFitRejectsInvalidConstraintsAndUnknownEnumValues() throws Exception {
        List<String> invalidRequests = List.of(
                """
                        {"budgetMax":0,"purpose":"PROGRAMMING","priority":"PERFORMANCE","limit":3}
                        """,
                """
                        {"budgetMax":100000000,"purpose":"PROGRAMMING","priority":"PERFORMANCE","minRamGb":-1,"limit":3}
                        """,
                """
                        {"budgetMax":100000000,"purpose":"UNKNOWN","priority":"PERFORMANCE","limit":3}
                        """,
                """
                        {"budgetMax":100000000,"purpose":"PROGRAMMING","priority":"PERFORMANCE","limit":6}
                        """
        );
        for (String request : invalidRequests) {
            HttpResponse<String> response = postSmartFit(request);
            assertEquals(400, response.statusCode(), "Request " + request + " returned " + response.body());
        }
    }

    private HttpResponse<String> get(String path) throws Exception {
        return client.send(request(path).GET().build(), HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> postSmartFit(String json) throws Exception {
        return client.send(request("/api/v1/smartfit/recommend").header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json)).build(), HttpResponse.BodyHandlers.ofString());
    }

    private HttpRequest.Builder request(String path) {
        // Deliberately omit Authorization, cookies and CSRF to exercise anonymous API access.
        return HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path)).timeout(Duration.ofSeconds(15));
    }
}
