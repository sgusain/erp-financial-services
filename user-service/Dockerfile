# syntax=docker/dockerfile:1

# ---- Stage 1: builder ----
FROM maven:3.9.6-eclipse-temurin-17-alpine AS builder
WORKDIR /build

# Copy every module's pom.xml first so the reactor graph resolves and the
# dependency-resolution layer caches independently of source changes. Maven's
# multi-module reactor validates that every module declared in the parent
# pom.xml physically exists (even ones -pl won't build), so all POMs must be
# present even though only common + this service's source gets copied below.
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

RUN mvn -pl user-service -am dependency:go-offline -B

COPY common/src common/src
COPY user-service/src user-service/src

RUN mvn -pl user-service -am clean package -DskipTests -B

# ---- Stage 2: runtime ----
FROM eclipse-temurin:17.0.11_9-jre-alpine

RUN addgroup -S appgroup && adduser -S appuser -G appgroup

WORKDIR /app

COPY --from=builder --chown=appuser:appgroup /build/user-service/target/*.jar app.jar

USER appuser

EXPOSE 8081

ENV JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseG1GC -XX:+ExitOnOutOfMemoryError -Duser.timezone=UTC"

HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8081/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
