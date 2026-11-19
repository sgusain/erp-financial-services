package com.erp.ai.security;

import java.util.Map;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

import com.erp.ai.config.ClientsProperties;

/**
 * Only for the test page (chat.html). It passes your username and password to the real
 * user-service login and returns its token. It never makes up tokens.
 * Turn it off with erp.ai.dev-login-enabled=false (do this in production).
 */
@RestController
@ConditionalOnProperty(name = "erp.ai.dev-login-enabled", havingValue = "true")
public class DevLoginController {

    private final RestClient restClient;

    public DevLoginController(ClientsProperties clients) {
        String base = (clients.userUrl() == null || clients.userUrl().isBlank())
                ? "http://localhost:8081" : clients.userUrl();
        this.restClient = RestClient.builder().baseUrl(base).build();
    }

    @PostMapping("/dev/login")
    public Map<String, Object> login(@RequestBody Map<String, String> credentials) {
        return restClient.post()
                .uri("/api/auth/login")
                .body(credentials)
                .retrieve()
                .body(new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {});
    }
}
