package com.erp.user.integration;

import com.erp.user.entity.Role;
import com.erp.user.repository.UserRepository;
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
 * Integration-level regression test of UserService.createUser's role-forcing logic
 * (unit-tested separately in UserServiceTest): even through the real HTTP/JSON/JPA
 * path, explicitly requesting "role":"ADMIN" on the public registration endpoint must
 * not result in an ADMIN account.
 */
class RegistrationRoleEscalationIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Test
    void registeringWithExplicitAdminRole_stillResultsInUserRole() throws Exception {
        String email = "escalation-" + UUID.randomUUID() + "@example.com";
        Map<String, Object> maliciousRequest = Map.of(
                "name", "Would-be Admin",
                "email", email,
                "password", "SuperSecret123",
                "role", "ADMIN");

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/users", AuthFlowIntegrationTest.jsonEntity(maliciousRequest), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        JsonNode body = objectMapper.readTree(response.getBody());
        assertThat(body.get("role").asText()).isEqualTo("USER");

        // Confirm it's not just the response DTO - the persisted row is USER too.
        assertThat(userRepository.findByEmail(email).orElseThrow().getRole()).isEqualTo(Role.USER);
    }
}
