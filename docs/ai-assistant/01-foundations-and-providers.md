# 01. Foundations and providers (Course 1 parts 2, 3)

## Goal
Create `ai-assistant-service` with a working chat endpoint. Switch between Claude, OpenAI, Gemini and Ollama by configuration only.

## Steps
0. Take the latest patch of Spring Boot 4.0.x, Spring AI 2.0.x and Spring Cloud 2025.1.2 (see 00-overview decision 2). Write the chosen versions at the top of `ai-assistant-service/pom.xml` as properties.
1. Create `ai-assistant-service/` (standalone pom, parent `spring-boot-starter-parent`, Java 21, packaging jar). Dependencies: web, actuator, validation, security, oauth2 resource server (for JWT, no jjwt), eureka client, config client, Flyway, postgresql, springdoc (use the Boot 4 compatible version), Spring AI starters for Anthropic, OpenAI, Google Gemini (GenAI) and Ollama. Avoid Lombok in this module (records and plain classes are easier to read). Test: Testcontainers, spring-security-test.
2. Add `erp-config/ai-assistant-service.yml` (port 8086, datasource `aidb`) and the service `application.yml` using the same `spring.config.import` pattern as other services.
3. Add `aidb` to `init-db.sql`. Switch the compose postgres image to `pgvector/pgvector:pg15`. Add an `ai-assistant-service` block to `docker-compose.yml` and `docker-compose.prod.yml` following budget-service. Add the gateway route `/api/ai/**` to `erp-config/api-gateway.yml`. Add a Dockerfile modeled on budget-service, but building only this standalone module, with Java 21 images.
4. JWT security with the resource-server decoder (see 00-overview decision 6). Test it with a token signed the same way user-service does (HS256).
5. Provider selection: property `erp.ai.provider` with values `anthropic`, `openai`, `gemini`, `ollama`. One `@Configuration` exposes a single `ChatClient` bean built from the selected provider's `ChatModel`. Use Spring profiles or `@ConditionalOnProperty`. Only the selected provider needs a key.
6. `POST /api/ai/chat` body `{ "message": "..." }` returns `{ "answer": "...", "provider": "..." }`. Validate the message (not blank, max 2000 characters).
7. Update the root README (service table, ports, routes).

## Done when
- Service starts, registers in Eureka, and `/actuator/health` is UP.
- Chat works through the gateway with a valid JWT and returns 401 without it.
- Changing only `erp.ai.provider` switches the model with no code change (show with Anthropic then Ollama).
- A unit test with a stub `ChatModel` covers the controller and service. No real provider is called in tests.
- Root `mvn clean install` is unchanged and passes.

## Interview notes
- An LLM predicts the next token and only sees what fits in its context window. Temperature controls randomness.
- "I would use ChatClient so one interface works for every provider and I switch by config. In my ERP assistant the provider is a single property."
