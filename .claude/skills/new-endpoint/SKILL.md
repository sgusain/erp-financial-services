---
name: new-endpoint
description: Add a REST endpoint to an ERP service following repo conventions (controller, service, DTO, validation, errors, tests, Swagger). Use when asked to add or extend an API endpoint.
---

# Add a REST endpoint

Follow these steps in the target service only. Look at a similar existing endpoint first (for example in `budget-service`) and copy its style.

1. **DTOs.** Request and response objects with validation annotations (`@NotBlank`, `@NotNull`, `@Positive`, `@Size`). Never expose entities directly.
2. **Service.** Business logic in the service class. Constructor injection. Throw the service's existing custom exceptions (not generic ones).
3. **Controller.** Thin. Use `@Valid` on request bodies. List endpoints take `Pageable` and return a page. Add `@Operation` Swagger annotations the same way other controllers do.
4. **Errors.** Make sure `GlobalExceptionHandler` returns the standard `ErrorResponse` for new exceptions.
5. **Database.** If the schema changes, add a new Flyway migration (next `V` number). Never change old migrations. `ddl-auto` stays `validate`.
6. **Security.** The endpoint must require a valid JWT unless it is explicitly public. Use the user from the token, never from the request body.
7. **Tests.** One unit test for the service (happy path and one failure path). One integration test with Testcontainers for the controller (status codes, validation error, unauthorized).
8. **Verify.** Run `mvn -pl <service> -am clean verify`. Fix failures. Do not claim done until it passes.
9. **Report.** List the files changed and the test result in two or three lines.

Do not edit other services. Keep the change small.
