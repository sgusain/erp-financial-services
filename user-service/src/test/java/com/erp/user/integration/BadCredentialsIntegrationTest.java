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
 * Regression test: bad login credentials must return 401 with a generic message,
 * not a 500 (this was a real bug fixed earlier in this project - AuthenticationException
 * previously fell through to the generic Exception handler).
 */
class BadCredentialsIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void login_withWrongPassword_returns401NotServerError() throws Exception {
        String email = "badcreds-" + UUID.randomUUID() + "@example.com";
        Map<String, Object> registerRequest = Map.of(
                "name", "Bad Creds User", "email", email, "password", "CorrectPassword123");
        restTemplate.postForEntity("/api/users", AuthFlowIntegrationTest.jsonEntity(registerRequest), String.class);

        Map<String, Object> loginRequest = Map.of("username", email, "password", "WrongPassword123");
        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/auth/login", AuthFlowIntegrationTest.jsonEntity(loginRequest), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        JsonNode body = objectMapper.readTree(response.getBody());
        assertThat(body.get("message").asText()).isEqualTo("Invalid username or password");
    }

    @Test
    void login_withUnknownUsername_returns401NotServerError() throws Exception {
        Map<String, Object> loginRequest = Map.of(
                "username", "does-not-exist-" + UUID.randomUUID() + "@example.com",
                "password", "WhateverPassword123");
        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/auth/login", AuthFlowIntegrationTest.jsonEntity(loginRequest), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
