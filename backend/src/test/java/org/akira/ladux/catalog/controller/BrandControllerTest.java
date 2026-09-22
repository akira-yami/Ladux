package org.akira.ladux.catalog.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.akira.ladux.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.web.server.LocalServerPort;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration test cho BrandController, chay tren server that (RANDOM_PORT) + Postgres (Testcontainers).
 *
 * Goi HTTP bang java.net.http.HttpClient (JDK built-in) + Jackson + JUnit assertions:
 * khong phu thuoc RestAssured (loi Groovy tren JDK 25) hay package test cua Spring (doi giua cac ban Boot).
 */
class BrandControllerTest extends AbstractIntegrationTest {

    @LocalServerPort
    private int port;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldReturnBrands() throws Exception {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/v1/brands"))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        // 1. API cong khai (GET) phai tra 200 OK.
        assertEquals(200, response.statusCode(), "GET /api/v1/brands phai tra 200");

        // 2. Body la mot Page -> co field "content" la mang, va co du lieu seed (devdata co 12 brand).
        JsonNode body = objectMapper.readTree(response.body());
        assertTrue(body.get("content").isArray(), "Field 'content' phai la mang");
        assertTrue(body.get("content").size() > 0, "Phai co it nhat 1 brand tu du lieu seed");
    }
}
