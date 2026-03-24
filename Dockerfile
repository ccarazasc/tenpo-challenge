# ── Stage 1: Build ────────────────────────────────────────────────────────
FROM maven:3.9-eclipse-temurin-21-alpine AS build

WORKDIR /build

# Cache dependencies before copying source (faster rebuilds)
COPY pom.xml .
RUN mvn dependency:go-offline -B -q

COPY src ./src
RUN mvn package -DskipTests -B -q

# ── Stage 2: Runtime ──────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Non-root user for security hardening
RUN addgroup -S tenpo && adduser -S tenpo -G tenpo
USER tenpo

COPY --from=build /build/target/*.jar app.jar

EXPOSE 8080

# JVM flags tuned for containers:
#   -XX:+UseContainerSupport   → respects cgroup memory/CPU limits
#   -XX:MaxRAMPercentage=75    → leaves headroom for OS and off-heap
ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.jar"]
