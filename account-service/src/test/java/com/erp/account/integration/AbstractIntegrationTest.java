package com.erp.account.integration;

import org.junit.jupiter.api.Tag;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
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

    // Same secret value used across all account-service integration tests so that
    // hand-crafted JWTs (built in tests, since account-service never issues tokens
    // itself) validate correctly against JwtValidator.
    public static final String TEST_JWT_SECRET =
            "test-only-secret-key-for-integration-tests-must-be-256-bits-long-enough";

    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>(DockerImageName.parse("postgres:15"))
                    .withDatabaseName("accountdb_test")
                    .withUsername("test")
                    .withPassword("test");

    // Needed because AccountService.getAccountById/applyBalanceChange are now
    // @Cacheable/@CacheEvict-backed by a real RedisCacheManager; without this,
    // AccountConcurrencyIntegrationTest's concurrent applyBalanceChange calls would try
    // to reach spring.data.redis's default localhost:6379 and fail wherever that isn't
    // actually running (e.g. CI).
    static final GenericContainer<?> REDIS =
            new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
                    .withExposedPorts(6379);

    static {
        REDIS.start();
        // See user-service's AbstractIntegrationTest for why this is necessary: the
        // JVM's regional default TimeZone ("Asia/Calcutta") is rejected outright by
        // Postgres as an invalid startup parameter. Must happen before any JDBC
        // connection is opened.
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        POSTGRES.start();
    }

    // Everything else (spring.cloud.config.enabled=false, eureka.client.enabled=false,
    // jwt.secret, ddl-auto, flyway baseline) now lives in
    // src/test/resources/application.yml instead of here. This is NOT just tidiness:
    // Spring Boot resolves "spring.config.import: configserver:..." during environment
    // preparation, which runs before the ApplicationContext exists - and therefore
    // before @DynamicPropertySource (a test-context customizer) ever gets a chance to
    // add its properties. Disabling Config Server via @DynamicPropertySource was too
    // late to matter: Spring Cloud Config had already attempted (and, without a real
    // Config Server reachable, failed with ConfigClientFailFastException) to fetch
    // remote config by the time this method ran. A classpath application.yml is
    // processed during that same early config-data phase, so it actually takes effect
    // in time. Only the datasource URL/credentials stay here, since they're genuinely
    // only known once Testcontainers assigns a port at runtime.
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
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
    }

}
