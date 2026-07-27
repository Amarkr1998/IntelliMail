# syntax=docker/dockerfile:1

# ---- Build stage --------------------------------------------------------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

# Copy only the POM first so dependency resolution is cached in its own layer
# and doesn't get invalidated by every source-code change.
COPY pom.xml .
RUN mvn -B dependency:go-offline

COPY src ./src
# Tests already run in CI (see .github/workflows/ci.yml) before this image is
# ever built, so skip them here to keep image builds fast and deterministic.
RUN mvn -B clean package -DskipTests

# ---- Runtime stage --------------------------------------------------------
FROM eclipse-temurin:21-jre-alpine AS runtime
WORKDIR /app

RUN addgroup -S intellimail && adduser -S intellimail -G intellimail
COPY --from=build /build/target/intellimail-backend.jar app.jar
RUN chown intellimail:intellimail app.jar
USER intellimail

EXPOSE 8080

# eclipse-temurin:21-jre-alpine ships busybox wget, not curl. Use 127.0.0.1
# explicitly, not "localhost" - it resolves to ::1 first in this container
# environment and Tomcat isn't listening on IPv6, so wget's first attempt
# would otherwise fail before ever trying the working IPv4 address.
# ${MANAGEMENT_SERVER_PORT:-8080}: when set (see application.yml), actuator
# moves entirely off the main port onto its own - this stays correct either way.
# (docker-compose.yml overrides this HEALTHCHECK for the compose-managed
# case; kept here too so a standalone `docker run` of this image still works.)
HEALTHCHECK --interval=15s --timeout=5s --retries=5 --start-period=60s \
  CMD wget --no-verbose --tries=1 --spider http://127.0.0.1:${MANAGEMENT_SERVER_PORT:-8080}/actuator/health || exit 1

# -XX:MaxRAMPercentage lets the JVM heap scale with whatever memory limit the
# container is actually given (see docker-compose.yml's mem_limit) rather than
# a fixed -Xmx that doesn't adapt across environments.
ENTRYPOINT ["sh", "-c", "java -XX:+UseContainerSupport -XX:MaxRAMPercentage=75 -jar app.jar"]
