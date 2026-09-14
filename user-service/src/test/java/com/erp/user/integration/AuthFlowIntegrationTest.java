package com.erp.user.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end: registration -> login -> access a protected endpoint -> refresh -> logout,
 * all against a real (Testcontainers) Postgres-backed Spring context.
 */
class AuthFlowIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void fullRegistrationLoginAccessRefreshLogoutFlow() throws Exception {
        String email = "flow-" + UUID.randomUUID() + "@example.com";
        String password = "SuperSecret123";

        // 1. Register
        Map<String, Object> registerRequest = Map.of(
                "name", "Flow User",
                "email", email,
                "password", password
        );
        ResponseEntity<String> registerResponse = restTemplate.postForEntity(
                "/api/users", jsonEntity(registerRequest), String.class);
        assertThat(registerResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        JsonNode registered = objectMapper.readTree(registerResponse.getBody());
        long userId = registered.get("id").asLong();
        assertThat(registered.get("role").asText()).isEqualTo("USER");

        // 2. Login
        Map<String, Object> loginRequest = Map.of("username", email, "password", password);
        ResponseEntity<String> loginResponse = restTemplate.postForEntity(
                "/api/auth/login", jsonEntity(loginRequest), String.class);
        assertThat(loginResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode loginBody = objectMapper.readTree(loginResponse.getBody());
        String accessToken = loginBody.get("token").asText();
        String refreshToken = loginBody.get("refreshToken").asText();
        assertThat(accessToken).isNotBlank();
        assertThat(refreshToken).isNotBlank();

        // 3. Access protected endpoint with the access token
        HttpHeaders authHeaders = bearerHeaders(accessToken);
        ResponseEntity<String> meResponse = restTemplate.exchange(
                "/api/users/" + userId, org.springframework.http.HttpMethod.GET,
                new HttpEntity<>(authHeaders), String.class);
        assertThat(meResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        // 4. Refresh
        Map<String, Object> refreshRequest = Map.of("refreshToken", refreshToken);
        ResponseEntity<String> refreshResponse = restTemplate.postForEntity(
                "/api/auth/refresh", jsonEntity(refreshRequest), String.class);
        assertThat(refreshResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode refreshBody = objectMapper.readTree(refreshResponse.getBody());
        String newRefreshToken = refreshBody.get("refreshToken").asText();
        assertThat(newRefreshToken).isNotEqualTo(refreshToken);

        // 5. Logout with the rotated refresh token
        Map<String, Object> logoutRequest = Map.of("refreshToken", newRefreshToken);
        ResponseEntity<String> logoutResponse = restTemplate.postForEntity(
                "/api/auth/logout", jsonEntity(logoutRequest), String.class);
        assertThat(logoutResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    static HttpEntity<Map<String, Object>> jsonEntity(Map<String, Object> body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(body, headers);
    }

    static HttpHeaders bearerHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return headers;
    }
}
