package com.erp.user.integration;

import com.erp.user.entity.Role;
import com.erp.user.entity.User;
import com.erp.user.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Role-based access control on admin-only user-management endpoints, exercised over
 * real HTTP with real JWTs issued by a real login, against a real Postgres-backed
 * context.
 */
class RbacIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    private String registerAndLogin(String rolePromotion) throws Exception {
        String email = "rbac-" + UUID.randomUUID() + "@example.com";
        String password = "SuperSecret123";

        Map<String, Object> registerRequest = Map.of(
                "name", "Rbac User", "email", email, "password", password);
        restTemplate.postForEntity("/api/users", AuthFlowIntegrationTest.jsonEntity(registerRequest), String.class);

        if (rolePromotion != null) {
            User user = userRepository.findByEmail(email).orElseThrow();
            user.setRole(Role.valueOf(rolePromotion));
            userRepository.save(user);
        }

        Map<String, Object> loginRequest = Map.of("username", email, "password", password);
        ResponseEntity<String> loginResponse = restTemplate.postForEntity(
                "/api/auth/login", AuthFlowIntegrationTest.jsonEntity(loginRequest), String.class);
        JsonNode body = objectMapper.readTree(loginResponse.getBody());
        return body.get("token").asText();
    }

    @Test
    void nonAdmin_getAllUsers_isForbidden() throws Exception {
        String token = registerAndLogin(null);

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/users", HttpMethod.GET,
                new HttpEntity<>(AuthFlowIntegrationTest.bearerHeaders(token)), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void admin_getAllUsers_succeeds() throws Exception {
        String token = registerAndLogin("ADMIN");

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/users", HttpMethod.GET,
                new HttpEntity<>(AuthFlowIntegrationTest.bearerHeaders(token)), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void nonAdmin_updateUser_isForbidden() throws Exception {
        String token = registerAndLogin(null);
        User target = userRepository.findAll().stream().findFirst().orElseThrow();

        HttpHeaders headers = AuthFlowIntegrationTest.bearerHeaders(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        Map<String, Object> updateRequest = Map.of(
                "name", "Hacked Name", "email", target.getEmail(), "password", "SomePassword123", "role", "USER");

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/users/" + target.getId(), HttpMethod.PUT,
                new HttpEntity<>(updateRequest, headers), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void nonAdmin_deleteUser_isForbidden() throws Exception {
        String token = registerAndLogin(null);
        User target = userRepository.findAll().stream().findFirst().orElseThrow();

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/users/" + target.getId(), HttpMethod.DELETE,
                new HttpEntity<>(AuthFlowIntegrationTest.bearerHeaders(token)), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void admin_updateUser_succeeds() throws Exception {
        String adminToken = registerAndLogin("ADMIN");
        String targetEmail = "rbac-target-" + UUID.randomUUID() + "@example.com";
        Map<String, Object> registerRequest = Map.of(
                "name", "Target User", "email", targetEmail, "password", "SuperSecret123");
        ResponseEntity<String> created = restTemplate.postForEntity(
                "/api/users", AuthFlowIntegrationTest.jsonEntity(registerRequest), String.class);
        long targetId = objectMapper.readTree(created.getBody()).get("id").asLong();

        HttpHeaders headers = AuthFlowIntegrationTest.bearerHeaders(adminToken);
        headers.setContentType(MediaType.APPLICATION_JSON);
        Map<String, Object> updateRequest = Map.of(
                "name", "Updated Name", "email", targetEmail, "password", "NewPassword123", "role", "USER");

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/users/" + targetId, HttpMethod.PUT,
                new HttpEntity<>(updateRequest, headers), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(objectMapper.readTree(response.getBody()).get("name").asText()).isEqualTo("Updated Name");
    }
}
