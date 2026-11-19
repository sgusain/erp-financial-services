# syntax=docker/dockerfile:1
# Standalone build: this module is not part of the root reactor, so the build
# context is just this folder (docker build ai-assistant-service).

# ---- Stage 1: builder ----
FROM maven:3.9.11-eclipse-temurin-21 AS builder
WORKDIR /build
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src src
RUN mvn clean package -DskipTests -B

# ---- Stage 2: runtime ----
FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
WORKDIR /app
COPY --from=builder --chown=appuser:appgroup /build/target/*.jar app.jar
RUN mkdir -p /app/logs && chown -R appuser:appgroup /app/logs
USER appuser
EXPOSE 8086
ENV JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseG1GC -XX:+ExitOnOutOfMemoryError -Duser.timezone=UTC"
HEALTHCHECK --interval=30s --timeout=10s --start-period=90s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8086/actuator/health || exit 1
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
