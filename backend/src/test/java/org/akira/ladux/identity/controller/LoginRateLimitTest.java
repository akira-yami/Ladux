package org.akira.ladux.identity.controller;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.UUID;
import org.akira.ladux.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.web.server.LocalServerPort;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * Integration test cho rate limit dang nhap (bucket4j + Redis).
 *
 * Cau hinh test: capacity=5 / phut. Goi POST /api/v1/auth/login 5 lan dau -> KHONG bi 429
 * (sai mat khau nen 401/400, nhung quan trong la khong bi chan). Lan thu 6 -> 429.
 * Moi request dung tai khoan khac nhau de chi do bucket IP, khong cham bucket IP + account (3 lan).
 */
class LoginRateLimitTest extends AbstractIntegrationTest {

    @LocalServerPort
    private int port;

    @Test
    void blocksLoginAfterExceedingLimit() throws Exception {
        HttpClient client = HttpClient.newHttpClient();
        String usernamePrefix = "rate-limit-test-" + UUID.randomUUID();

        // 5 lan dau: trong gioi han -> khong bi 429.
        for (int i = 1; i <= 5; i++) {
            int status = postLogin(client, usernamePrefix + "-" + i);
            assertNotEquals(429, status, "Lan thu " + i + " khong duoc bi rate limit");
        }

        // Lan thu 6: vuot gioi han -> 429.
        int sixth = postLogin(client, usernamePrefix + "-6");
        assertEquals(429, sixth, "Lan thu 6 phai bi chan (429 Too Many Requests)");
    }

    private int postLogin(HttpClient client, String username) throws Exception {
        String body = "{\"username\":\"" + username + "\",\"password\":\"sai-mat-khau\"}";
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/v1/auth/login"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        return client.send(request, HttpResponse.BodyHandlers.ofString()).statusCode();
    }
}
