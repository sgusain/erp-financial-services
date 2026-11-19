package com.erp.ai;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import com.erp.ai.service.PendingActionService;
import com.erp.ai.support.StubAiConfig;
import com.erp.ai.support.TokenFactory;

/**
 * Starts the whole service with a real Postgres (pgvector) and Redis in Docker.
 * The model is a stub, so no API key is needed. Skipped automatically when Docker is not running.
 */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.ai.model.chat=none",
        "spring.ai.model.embedding=none",
        "erp.ai.intent-routing-enabled=false",
        "erp.ai.dev-login-enabled=false",
        "spring.cloud.config.enabled=false",
        "spring.config.import=",
        "eureka.client.enabled=false",
        "jwt.secret=" + TokenFactory.SECRET
})
@Import(StubAiConfig.class)
class AiAssistantApiTest {

    @Container
    static PostgreSQLContainer postgres =
            new PostgreSQLContainer(DockerImageName.parse("pgvector/pgvector:pg15").asCompatibleSubstituteFor("postgres"));

    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);

    @DynamicPropertySource
    static void containers(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
    }

    @LocalServerPort
    int port;

    @Autowired
    PendingActionService pendingActions;

    private final HttpClient http = HttpClient.newHttpClient();

    @Test
    void requestWithoutTokenIsRejected() throws Exception {
        HttpResponse<String> response = http.send(
                HttpRequest.newBuilder(URI.create(url("/api/ai/usage"))).GET().build(),
                HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(401);
    }

    @Test
    void chatReturnsTheModelAnswerAndCreatesAChat() throws Exception {
        HttpResponse<String> response = post("/api/ai/chat", "{\"message\":\"Hello\"}", "sandy", "USER");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains(StubAiConfig.STUB_ANSWER).contains("chatId");
    }

    @Test
    void emptyMessageIsABadRequest() throws Exception {
        HttpResponse<String> response = post("/api/ai/chat", "{\"message\":\"\"}", "sandy", "USER");

        assertThat(response.statusCode()).isEqualTo(400);
    }

    @Test
    void ingestIsAdminOnly() throws Exception {
        HttpResponse<String> response = post("/api/ai/policy/ingest", "", "sandy", "USER");

        assertThat(response.statusCode()).isEqualTo(403);
    }

    @Test
    void pendingActionsAreVisibleOnlyToTheirOwner() {
        pendingActions.create("alice", 1L, new BigDecimal("5000.00"), "test");

        assertThat(pendingActions.listPending("alice", PageRequest.of(0, 10)).getTotalElements()).isEqualTo(1);
        assertThat(pendingActions.listPending("bob", PageRequest.of(0, 10)).getTotalElements()).isZero();
    }

    private HttpResponse<String> post(String path, String body, String user, String role) throws Exception {
        return http.send(HttpRequest.newBuilder(URI.create(url(path)))
                        .header("Authorization", "Bearer " + TokenFactory.token(user, role))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(body))
                        .build(),
                HttpResponse.BodyHandlers.ofString());
    }

    private String url(String path) {
        return "http://localhost:" + port + path;
    }
}
