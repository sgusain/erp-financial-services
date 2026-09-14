package com.erp.user.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Refresh token rotation: each refresh token is single-use. Using an already-rotated
 * (i.e. now-revoked) refresh token a second time must fail rather than silently
 * issuing another token pair - otherwise a stolen refresh token could be replayed
 * indefinitely.
 */
class RefreshTokenRotationIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void reusingARotatedAwayRefreshToken_fails() throws Exception {
        String email = "rotation-" + UUID.randomUUID() + "@example.com";
        String password = "SuperSecret123";
        restTemplate.postForEntity("/api/users", AuthFlowIntegrationTest.jsonEntity(
                Map.of("name", "Rotation User", "email", email, "password", password)), String.class);

        ResponseEntity<String> loginResponse = restTemplate.postForEntity(
                "/api/auth/login", AuthFlowIntegrationTest.jsonEntity(
                        Map.of("username", email, "password", password)), String.class);
        String originalRefreshToken = objectMapper.readTree(loginResponse.getBody()).get("refreshToken").asText();

        // First use: succeeds and rotates to a new token.
        ResponseEntity<String> firstRefresh = restTemplate.postForEntity(
                "/api/auth/refresh", AuthFlowIntegrationTest.jsonEntity(
                        Map.of("refreshToken", originalRefreshToken)), String.class);
        assertThat(firstRefresh.getStatusCode()).isEqualTo(HttpStatus.OK);

        // Second use of the SAME original token: must fail now that it's revoked.
        ResponseEntity<String> secondRefresh = restTemplate.postForEntity(
                "/api/auth/refresh", AuthFlowIntegrationTest.jsonEntity(
                        Map.of("refreshToken", originalRefreshToken)), String.class);
        assertThat(secondRefresh.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void refreshingWithAnUnknownToken_fails() {
        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/auth/refresh", AuthFlowIntegrationTest.jsonEntity(
                        Map.of("refreshToken", "not-a-real-token-" + UUID.randomUUID())), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }
}
