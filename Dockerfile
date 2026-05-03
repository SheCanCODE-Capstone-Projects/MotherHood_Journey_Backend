#  Stage 1 – BUILD  (Maven + JDK 21, cached dependencies layer)
FROM maven:3.9.6-eclipse-temurin-21-alpine AS builder

WORKDIR /build

# Copy POM first → Docker caches dependency download layer
COPY pom.xml .
RUN mvn dependency:go-offline -q

# Copy source and build the fat-jar (skip tests here; tests run in CI)
COPY src ./src
RUN mvn package -DskipTests -q

#  Stage 2 – RUNTIME  (minimal JRE only – no Maven, no JDK)

FROM eclipse-temurin:21-jre-alpine AS runtime

# Security: run as non-root user
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

WORKDIR /app

# Copy only the fat-jar from the builder stage
COPY --from=builder /build/target/motherhood-journey-*.jar app.jar

# Ownership
RUN chown appuser:appgroup app.jar

USER appuser

# Expose application port
EXPOSE 8080

# ── JVM tuning: container-aware GC, fast startup ──────────────────
ENV JAVA_OPTS="-XX:+UseContainerSupport \
               -XX:MaxRAMPercentage=75.0 \
               -XX:+UseG1GC \
               -Djava.security.egd=file:/dev/./urandom"

# ── Healthcheck (Docker built-in) ─────────────────────────────────
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD wget -qO- http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]