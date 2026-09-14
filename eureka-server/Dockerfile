# syntax=docker/dockerfile:1

# ---- Stage 1: builder ----
FROM maven:3.9.6-eclipse-temurin-17-alpine AS builder
WORKDIR /build

# Copy every module's pom.xml first so the reactor graph resolves and the
# dependency-resolution layer caches independently of source changes. Maven's
# multi-module reactor validates that every module declared in the parent
# pom.xml physically exists (even ones -pl won't build), so all POMs must be
# present even though only this service's source gets copied below.
# eureka-server does NOT depend on the common module, so common/src is not copied.
COPY pom.xml .
COPY common/pom.xml common/pom.xml
COPY eureka-server/pom.xml eureka-server/pom.xml
COPY config-server/pom.xml config-server/pom.xml
COPY api-gateway/pom.xml api-gateway/pom.xml
COPY user-service/pom.xml user-service/pom.xml
COPY account-service/pom.xml account-service/pom.xml
COPY budget-service/pom.xml budget-service/pom.xml
COPY transaction-service/pom.xml transaction-service/pom.xml
COPY notification-service/pom.xml notification-service/pom.xml

RUN mvn -pl eureka-server -am dependency:go-offline -B

COPY eureka-server/src eureka-server/src

RUN mvn -pl eureka-server -am clean package -DskipTests -B

# ---- Stage 2: runtime ----
FROM eclipse-temurin:17.0.11_9-jre-alpine

RUN addgroup -S appgroup && adduser -S appuser -G appgroup

WORKDIR /app

COPY --from=builder --chown=appuser:appgroup /build/eureka-server/target/*.jar app.jar

USER appuser

EXPOSE 8761

ENV JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseG1GC -XX:+ExitOnOutOfMemoryError -Duser.timezone=UTC"

# eureka-server has no datasource/Flyway migration to wait on at startup (unlike
# the DB-backed business services), so it comes up faster in practice; the
# start-period is kept at 60s anyway for consistency with the other services.
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8761/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
