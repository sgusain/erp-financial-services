# CLAUDE.md

ERP Financial Services: Spring Boot microservices (Maven multi-module). Learning and interview project.

## Stack
Java 17, Spring Boot 3.2.5, Spring Cloud 2023.0.1, PostgreSQL 15, Kafka, Redis, Docker, Kubernetes, GitHub Actions to AWS ECR.
Exception: `ai-assistant-service/` is standalone (Java 21, Spring Boot 4.0.x, Spring AI 2.0.x, Spring Cloud 2025.1.2) (see docs/ai-assistant/00-overview.md). It is not in the root `<modules>`.

## Services and ports
eureka-server 8761, config-server 8888, api-gateway 8080, user-service 8081, account-service 8082, budget-service 8083, transaction-service 8084, notification-service 8085, ai-assistant-service 8086.

## Commands
- Build and test everything: `mvn clean install`
- One service: `mvn -pl budget-service -am clean verify`
- AI service: `mvn -f ai-assistant-service/pom.xml clean verify`
- Local infra: `docker compose up -d postgres redis kafka eureka-server config-server`

## Rules
- Constructor injection only. No field `@Autowired`.
- Every controller uses `@Valid`. List endpoints use `Pageable`.
- Schema changes only through Flyway migrations. `ddl-auto` stays `validate`.
- Each service has a `GlobalExceptionHandler` returning the standard `ErrorResponse`.
- Add or update tests with every change. Integration tests use Testcontainers.
- Never commit secrets. Keys come from environment variables.
- Do not edit other services when working on a spec unless the spec says so.
- Keep changes small. One spec per commit.

## AI assistant specs
Specs are in `docs/ai-assistant/`. Start with `README.md` and `00-overview.md`.
