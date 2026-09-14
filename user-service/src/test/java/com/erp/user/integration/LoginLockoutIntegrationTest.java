package com.erp.user.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Login lockout: 5 consecutive failed logins lock the account for a cooldown period;
 * a 6th attempt - even with the CORRECT password - must be rejected with 423 Locked
 * while the lockout is in effect.
 */
class LoginLockoutIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void fiveFailedLogins_thenCorrectPassword_returns423Locked() {
        String email = "lockout-" + UUID.randomUUID() + "@example.com";
        String correctPassword = "CorrectPassword123";
        restTemplate.postForEntity("/api/users", AuthFlowIntegrationTest.jsonEntity(
                Map.of("name", "Lockout User", "email", email, "password", correctPassword)), String.class);

        for (int i = 0; i < 5; i++) {
            ResponseEntity<String> failedLogin = restTemplate.postForEntity(
                    "/api/auth/login", AuthFlowIntegrationTest.jsonEntity(
                            Map.of("username", email, "password", "WrongPassword" + i)), String.class);
            assertThat(failedLogin.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }

        ResponseEntity<String> sixthAttempt = restTemplate.postForEntity(
                "/api/auth/login", AuthFlowIntegrationTest.jsonEntity(
                        Map.of("username", email, "password", correctPassword)), String.class);

        assertThat(sixthAttempt.getStatusCode()).isEqualTo(HttpStatus.LOCKED);
    }
}
