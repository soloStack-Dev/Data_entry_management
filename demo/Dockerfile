# syntax=docker/dockerfile:1
#
# Data Entry Management Platform - production image.
# Build from the repository root with:  docker compose up --build
# The compose build context is this directory, which is why the paths below are
# all relative to demo/.

# =============================================================================
# Stage 1 - build
# A full Maven + JDK image compiles the jar. It is thrown away afterwards, so the
# 200 MB of build tooling never reaches the final image.
# =============================================================================
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /build

# pom.xml is copied on its own first. This layer is only rebuilt when the
# dependencies change, so ordinary code edits reuse the cached Maven repository
# and the build stays fast.
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

# The real sources.
COPY src ./src

# Package without tests: the suite needs a live MySQL, which does not exist
# while the image is being built. Tests are run from your machine instead.
RUN mvn -B -q clean package -DskipTests

# =============================================================================
# Stage 2 - runtime
# A JRE-only image: no compiler, no Maven, no sources.
# =============================================================================
FROM eclipse-temurin:21-jre-alpine

# curl is used by the HEALTHCHECK below.
RUN apk add --no-cache curl

# Run as a non-root user. A container process that does not need root should not
# have it, and nothing here does.
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

WORKDIR /app

# The layered jar is the only artefact copied into the runtime image.
COPY --from=build --chown=spring:spring /build/target/*.jar app.jar

EXPOSE 8080

# The container is reported unhealthy if the actuator health endpoint - which
# includes the database connection - stops answering, so an orchestrator or
# `docker compose ps` can see the real state of the application.
HEALTHCHECK --interval=15s --timeout=5s --start-period=45s --retries=5 \
	CMD curl -fsS http://localhost:8080/actuator/health || exit 1

# Overridable with `docker run -e SERVER_PORT=...`. This is the port *inside*
# the container, which is why it stays 8080 even though the host port is 8081 -
# compose publishes 8081:8080.
ENV SERVER_PORT=8080 \
	JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:+UseContainerSupport"

# exec form via sh -c so JAVA_OPTS is word-split into separate JVM arguments.
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
