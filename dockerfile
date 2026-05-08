# ── Stage 1: Build with Maven ────────────────────────────────────────────────
FROM maven:3.9-eclipse-temurin-21-alpine AS builder

WORKDIR /build

# Copy pom first for layer caching — dependencies are re-downloaded only when pom changes
COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2 \
    mvn dependency:go-offline -B

COPY src ./src

RUN --mount=type=cache,target=/root/.m2 \
    mvn clean package -DskipTests -B && \
    java -Djarmode=layertools -jar target/*.jar extract --destination /build/layers

# ── Stage 2: Minimal JRE runtime ─────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine

# Non-root user for security
RUN addgroup -S mysticmart && adduser -S mysticmart -G mysticmart

WORKDIR /app

# Copy exploded layers in order of least→most frequently changed.
# This maximises Docker layer cache reuse on redeploy.
COPY --from=builder /build/layers/dependencies/          ./
COPY --from=builder /build/layers/spring-boot-loader/    ./
# snapshot-dependencies is empty for release builds; kept for dev parity
COPY --from=builder /build/layers/snapshot-dependencies/ ./
COPY --from=builder /build/layers/application/           ./

# JAVA_TOOL_OPTIONS is read natively by the JVM — no shell interpolation needed.
# UseContainerSupport + MaxRAMPercentage replaces the old -Xmx approach and
# correctly respects cgroup memory limits set by Docker / Kubernetes.
ENV JAVA_TOOL_OPTIONS="\
    -XX:+UseContainerSupport \
    -XX:MaxRAMPercentage=75.0 \
    -XX:+UseG1GC \
    -Djava.security.egd=file:/dev/./urandom"

# Documents the port the app listens on.
# Actual binding is controlled by -p flags or the orchestrator (e.g. K8s Service).
EXPOSE 8080

USER mysticmart

# Java runs as PID 1 so it receives SIGTERM directly on `docker stop`,
# allowing Spring Boot's graceful shutdown to complete cleanly.
#
# JarLauncher path is correct for Spring Boot ≥ 3.2.
# For Spring Boot 3.1 and below, use:
#   org.springframework.boot.loader.JarLauncher
ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]