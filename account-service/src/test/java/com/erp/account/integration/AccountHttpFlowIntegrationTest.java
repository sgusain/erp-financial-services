package com.erp.account.integration;

import com.erp.common.enums.TransactionType;
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
 * Full HTTP flow: create an account, adjust its balance via PUT .../{id}/balance,
 * then confirm the new balance via GET - against a real Testcontainers Postgres.
 */
class AccountHttpFlowIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    private HttpEntity<Map<String, Object>> jsonEntity(Map<String, Object> body, HttpHeaders headers) {
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(body, headers);
    }

    @Test
    void createAccount_thenAdjustBalance_thenConfirmViaGet() throws Exception {
        HttpHeaders userHeaders = TestJwtSupport.bearerHeaders("flow-user@example.com", "USER");

        Map<String, Object> createRequest = Map.of(
                "accountCode", "FLOW-" + UUID.randomUUID(),
                "accountName", "Flow Test Account",
                "accountType", "ASSET",
                "balance", 100);
        ResponseEntity<String> createResponse = restTemplate.exchange(
                "/api/accounts", HttpMethod.POST, jsonEntity(createRequest, userHeaders), String.class);
        assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        JsonNode created = objectMapper.readTree(createResponse.getBody());
        long id = created.get("id").asLong();

        Map<String, Object> adjustRequest = Map.of("type", TransactionType.CREDIT.name(), "amount", 50);
        ResponseEntity<String> adjustResponse = restTemplate.exchange(
                "/api/accounts/" + id + "/balance", HttpMethod.PUT,
                jsonEntity(adjustRequest, TestJwtSupport.bearerHeaders("flow-user@example.com", "USER")), String.class);
        assertThat(adjustResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode adjusted = objectMapper.readTree(adjustResponse.getBody());
        assertThat(adjusted.get("balance").decimalValue()).isEqualByComparingTo("150.00");

        ResponseEntity<String> getResponse = restTemplate.exchange(
                "/api/accounts/" + id, HttpMethod.GET,
                new HttpEntity<>(TestJwtSupport.bearerHeaders("flow-user@example.com", "USER")), String.class);
        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode fetched = objectMapper.readTree(getResponse.getBody());
        assertThat(fetched.get("balance").decimalValue()).isEqualByComparingTo("150.00");
    }

    @Test
    void accessWithoutAuthorizationHeader_isRejected() {
        ResponseEntity<String> response = restTemplate.getForEntity("/api/accounts", String.class);

        assertThat(response.getStatusCode()).isIn(HttpStatus.UNAUTHORIZED, HttpStatus.FORBIDDEN);
    }

    @Test
    void deleteAccount_asNonAdmin_isForbidden() throws Exception {
        HttpHeaders userHeaders = TestJwtSupport.bearerHeaders("del-user@example.com", "USER");
        Map<String, Object> createRequest = Map.of(
                "accountCode", "DEL-" + UUID.randomUUID(),
                "accountName", "Delete Test Account",
                "accountType", "ASSET",
                "balance", 0);
        ResponseEntity<String> createResponse = restTemplate.exchange(
                "/api/accounts", HttpMethod.POST, jsonEntity(createRequest, userHeaders), String.class);
        long id = objectMapper.readTree(createResponse.getBody()).get("id").asLong();

        ResponseEntity<String> deleteResponse = restTemplate.exchange(
                "/api/accounts/" + id, HttpMethod.DELETE,
                new HttpEntity<>(TestJwtSupport.bearerHeaders("del-user@example.com", "USER")), String.class);

        assertThat(deleteResponse.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void deleteAccount_asAdmin_succeeds() throws Exception {
        HttpHeaders userHeaders = TestJwtSupport.bearerHeaders("admin-del@example.com", "ADMIN");
        Map<String, Object> createRequest = Map.of(
                "accountCode", "ADEL-" + UUID.randomUUID(),
                "accountName", "Admin Delete Test Account",
                "accountType", "ASSET",
                "balance", 0);
        ResponseEntity<String> createResponse = restTemplate.exchange(
                "/api/accounts", HttpMethod.POST, jsonEntity(createRequest, userHeaders), String.class);
        long id = objectMapper.readTree(createResponse.getBody()).get("id").asLong();

        ResponseEntity<String> deleteResponse = restTemplate.exchange(
                "/api/accounts/" + id, HttpMethod.DELETE,
                new HttpEntity<>(TestJwtSupport.bearerHeaders("admin-del@example.com", "ADMIN")), String.class);

        assertThat(deleteResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }
}
