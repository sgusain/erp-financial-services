# 00. Overview, decisions, conventions

## Goal

Add an AI chat assistant to the ERP. Users ask about balances, transactions, budgets and policies. The assistant answers from real ERP data, can run safe actions, and exposes ERP tools over MCP.

## Decisions (read before any spec)

1. **Separate module, not part of the root reactor.** Create `ai-assistant-service/` as a standalone Maven project with its own parent. Do NOT add it to `<modules>` in the root `pom.xml`. Reason: every existing Dockerfile copies all module poms, and `ci.yml` runs `mvn clean install` on the reactor. Adding a Spring Boot 4 module there would break 107 passing tests and all Docker builds.
2. **Versions.** Course 1 uses Spring AI 2.0 on Spring Boot 4. Step 0 of spec 01: confirm the current compatible versions (Spring Boot 4.x, Spring AI 2.0.x, and the Spring Cloud release train that supports that Boot line) from spring.io. Do not guess versions. Fallback if Eureka or Config client is not yet compatible: Spring Boot 3.5.x with Spring AI 1.1.x. The ChatClient code is almost the same.
3. **Java.** Java 17 minimum (same as the repo). Build JDK can be newer.
4. **Port** 8086. Eureka name `AI-ASSISTANT-SERVICE`. Gateway path `/api/ai/**`.
5. **Database** `aidb` on the existing Postgres, with the pgvector extension. The stock `postgres:15` image has no pgvector. Use the `pgvector/pgvector:pg15` image for the compose service (change the image only; data volume stays compatible with PG 15).
6. **Auth.** Same JWT as other services. Copy the pattern from `budget-service/.../security` (`JwtValidator`, `JwtAuthenticationFilter`, `SecurityConfig`) including the Redis blacklist check if the other services have it. Secret from `jwt.secret` (env `JWT_SECRET`). The user id for every tool call and memory key comes from the validated token, never from the request body or the model.
7. **Config** goes in `erp-config/ai-assistant-service.yml` like the other services. Secrets (API keys) only from environment variables, never committed. Never write keys into `.env` that is tracked.
8. **Conventions to copy from existing services:** `GlobalExceptionHandler` with the standard `ErrorResponse`, `@Valid` on controllers, Pageable on list endpoints, Flyway with `ddl-auto: validate`, `CorrelationIdFilter` and structured JSON logging, springdoc OpenAPI, Testcontainers for integration tests, constructor injection.
9. **Provider keys:** `ANTHROPIC_API_KEY`, `OPENAI_API_KEY`, `GEMINI_API_KEY`. Local: Ollama at `http://localhost:11434`. Tests must never call a real provider. Use a stub `ChatModel` or Ollama-free mocks.

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
