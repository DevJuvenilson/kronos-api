# syntax=docker/dockerfile:1.7

# The project is compiled for Java 25 (see pom.xml), so the build and runtime
# stages intentionally use the same Java major version.
FROM maven:3.9.11-eclipse-temurin-25-alpine AS build

WORKDIR /workspace

# Resolve dependencies separately to maximize Docker layer reuse.
COPY pom.xml ./
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -ntp dependency:go-offline

COPY src ./src
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -ntp clean package -DskipTests \
    && cp target/api-*.jar /workspace/app.jar


FROM eclipse-temurin:25-jre-alpine AS runtime

# Use fixed numeric IDs to make file ownership predictable across environments.
RUN addgroup -S -g 10001 kronos \
    && adduser -S -D -H -u 10001 -G kronos -s /sbin/nologin kronos

WORKDIR /app

COPY --from=build --chown=kronos:kronos /workspace/app.jar ./app.jar

USER 10001:10001

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
