# 00. Overview, decisions, conventions

## Goal

Add an AI chat assistant to the ERP. Users ask about balances, transactions, budgets and policies. The assistant answers from real ERP data, can run safe actions, and exposes ERP tools over MCP.

## Decisions (read before any spec)

1. **Separate module, not part of the root reactor.** Create `ai-assistant-service/` as a standalone Maven project with its own parent. Do NOT add it to `<modules>` in the root `pom.xml`. Reason: every existing Dockerfile copies all module poms, and `ci.yml` runs `mvn clean install` on the reactor. Adding a Spring Boot 4 module there would break 107 passing tests and all Docker builds.
2. **Versions (decided, verified Nov 2026).** Spring Boot **4.0.x**, Spring AI **2.0.x** (GA since June 2026; it supports Boot 4.0.x and 4.1.x), Spring Cloud **2025.1.2** (compatible with Boot 4.0.7 and 4.1.0). This is the same stack as Course 1, so you can follow the videos with no translation. Spring AI 2.0 does not run on Boot 3.x, so the existing services cannot share it. Spring Boot 3.5 reached end of life on 2026-06-30, which is another reason not to build new code on it. At implementation time, take the latest patch of each line from start.spring.io and write the versions in the pom properties. Do not use milestone or RC versions. If the Eureka or Config client fails to start on this stack, drop them for the first version (set a fixed URL for the gateway route) and note it in the PR. Do not downgrade to Boot 3.x.
3. **Java 21 for this module only.** Spring AI 2.0 supports Java 17 as the minimum but recommends 21. Use 21 in this module's pom and Dockerfile (`eclipse-temurin:21-jre-alpine`). Other services stay on 17. Keep the code itself simple (records, no preview features).
4. **Port** 8086. Eureka name `AI-ASSISTANT-SERVICE`. Gateway path `/api/ai/**`.
5. **Database** `aidb` on the existing Postgres, with the pgvector extension. The stock `postgres:15` image has no pgvector. Use the `pgvector/pgvector:pg15` image for the compose service (change the image only; data volume stays compatible with PG 15).
6. **Auth (simplest option).** Same JWT as other services (HS256, signed by user-service with `jwt.secret`, subject = username, claim `role`). Do NOT copy the jjwt classes from the other services: Boot 4 uses Jackson 3 and jjwt needs Jackson 2, which causes classpath trouble. Use Spring Security's built-in resource server instead: add `spring-boot-starter-oauth2-resource-server` and create one `JwtDecoder` bean with `NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build()` where `key = new SecretKeySpec(secret.getBytes(), "HmacSHA256")` (same bytes the other services use). Map the `role` claim to an authority in a small `JwtAuthenticationConverter`. That is about 30 lines in total in `SecurityConfig`. Secret from `jwt.secret` (env `JWT_SECRET`). "userId" in these specs means the JWT subject (the username). It comes from the validated token, never from the request body or the model.
7. **Config** goes in `erp-config/ai-assistant-service.yml` like the other services. Secrets (API keys) only from environment variables, never committed. Never write keys into `.env` that is tracked.
8. **Conventions to copy from existing services:** `GlobalExceptionHandler` with the standard `ErrorResponse`, `@Valid` on controllers, Pageable on list endpoints, Flyway with `ddl-auto: validate`, `CorrelationIdFilter` and structured JSON logging, springdoc OpenAPI, Testcontainers for integration tests, constructor injection.
9. **Provider keys:** `ANTHROPIC_API_KEY`, `OPENAI_API_KEY`, `GEMINI_API_KEY`. Local: Ollama at `http://localhost:11434`. Tests must never call a real provider. Use a stub `ChatModel` or Ollama-free mocks.

## Code style: keep it easy to understand

The goal is code you can explain in an interview. Follow these rules in every spec:

- Simple layers: controller, then service, then client or repository. No extra abstraction layers, no generic base classes, no custom frameworks.
- Prefer plain Java and Spring AI's own classes over clever helpers. One class does one job. Keep methods short (about 20 lines).
- DTOs are Java records. Constructor injection. No Lombok magic beyond what the other services already use.
- Name things by what they do: `PiiMaskingAdvisor`, `BudgetAnalystAgent`, `ErpTools`.
- Comment the "why" for anything AI-specific (why this advisor order, why this step limit, why this threshold). Do not comment obvious code.
- Prompts live in files under `prompts/`, not in Java strings.
- No reactive code except the single streaming endpoint (spec 11).
- Config values (limits, thresholds, provider) live in `application.yml` under the `erp.ai` prefix, with one `@ConfigurationProperties` record.
- Each spec adds its own tests. Tests use a stub `ChatModel`, so they are fast and never call a provider.

## Target package layout

```
ai-assistant-service/
  pom.xml
  Dockerfile
  src/main/java/com/erp/ai/
    AiAssistantApplication.java
    config/        (ChatClient beans, OpenAPI, provider profiles)
    controller/    (ChatController, SearchController, IngestController)
    service/       (ChatService, IngestionService, CategorizationService)
    advisor/       (PiiMaskingAdvisor, TokenUsageAdvisor)
    tool/          (ErpTools, client/ Feign clients)
    agent/         (BudgetAnalystAgent)
    mcp/           (MCP server tool beans)
    security/      (copied JWT pattern)
    exception/     filter/ dto/
  src/main/resources/
    application.yml, logback-spring.xml
    prompts/       (*.st templates, system prompts)
    db/migration/  (V1__...)
    policies/      (sample policy docs for RAG)
```

## Definition of done for every spec

- Code compiles with `mvn -f ai-assistant-service/pom.xml clean verify`.
- New tests pass. No real LLM calls in tests.
- Existing root build still passes: `mvn clean install` at repo root (unchanged).
- No secrets in git. Swagger shows any new endpoint.
- README table in the repo root updated when a new port, route or service is added (spec 01 only).
