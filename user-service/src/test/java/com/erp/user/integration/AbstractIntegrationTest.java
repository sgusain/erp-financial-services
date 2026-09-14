package com.erp.user.integration;

import org.junit.jupiter.api.Tag;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.TimeZone;

/**
 * Base class for full Spring context integration tests backed by a real, throwaway
 * Testcontainers Postgres instance. Deliberately isolates the test context from live
 * infrastructure (config-server on 8888, Eureka on 8761) so tests are fast and
 * deterministic and never touch the developer's real Postgres/Kafka/etc containers.
 *
 * Uses the Testcontainers "singleton container" pattern: the container is started
 * once (manually, in a static initializer) and shared across every integration test
 * class in this module within the same JVM fork, rather than being annotated
 * with @Container/@Testcontainers - which would start/stop it per test class and
 * break once a later class tried to reuse an already-stopped container. Testcontainers'
 * Ryuk resource reaper cleans it up when the JVM exits.
 */
@Tag("integration")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class AbstractIntegrationTest {

    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>(DockerImageName.parse("postgres:15"))
                    .withDatabaseName("userdb_test")
                    .withUsername("test")
                    .withPassword("test");

    static {
        // The pgjdbc driver sends the JVM's default TimeZone as a Postgres startup
        // parameter. On this host the JVM reports the legacy Java zone ID
        // "Asia/Calcutta" (an alias for Asia/Kolkata), which Postgres's GUC parser
        // rejects outright ("FATAL: invalid value for parameter \"TimeZone\""), failing
        // the connection before any query ever runs. Forcing the JVM default to a zone
        // Postgres recognizes fixes this at the source, regardless of the machine's
        // regional settings. Must happen before any JDBC connection is opened.
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        // Postgres JDBC driver otherwise sends the JVM's default TimeZone (e.g. the
        // legacy ID "Asia/Calcutta"), which some Postgres builds reject as an invalid
        // TimeZone startup parameter. Mirrors the "?TimeZone=UTC" already used in
        // D:\erp-config\{user,account}-service.yml datasource URLs for the same reason.
        // Built directly from host/port/database rather than appending to
        // getJdbcUrl() to avoid any ambiguity over whether it already has a query string.
        registry.add("spring.datasource.url", () -> "jdbc:postgresql://" + POSTGRES.getHost() + ":"
                + POSTGRES.getMappedPort(5432) + "/" + POSTGRES.getDatabaseName() + "?TimeZone=UTC");
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);

        // Isolate from live infra - never hit config-server or Eureka during tests.
        registry.add("spring.config.import", () -> "");
        registry.add("spring.cloud.config.enabled", () -> "false");
        registry.add("eureka.client.enabled", () -> "false");

        // Mirrors D:\erp-config behavior: schema must be pre-created (Flyway) before
        // Hibernate validates against it.
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("spring.flyway.baseline-on-migrate", () -> "true");
        registry.add("spring.flyway.baseline-version", () -> "1");

        // jwt.* normally comes from the shared config-server application.yml; supply
        // directly since config-server import is disabled for tests.
        registry.add("jwt.secret", () -> "test-only-secret-key-for-integration-tests-must-be-256-bits-long-enough");
        registry.add("jwt.expiration", () -> "900000");
        registry.add("jwt.refresh-expiration", () -> "604800000");

        // Generous rate limit so lockout/retry-heavy tests aren't flaky against the
        // @RateLimiter on /api/auth/login.
        registry.add("resilience4j.ratelimiter.instances.login.limit-for-period", () -> "10000");
        registry.add("resilience4j.ratelimiter.instances.login.limit-refresh-period", () -> "1s");
        registry.add("resilience4j.ratelimiter.instances.login.timeout-duration", () -> "0s");
    }

}
