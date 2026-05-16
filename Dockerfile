#  Stage 1 – BUILD  (Maven + JDK 21, cached dependencies layer)
FROM maven:3.9.6-eclipse-temurin-21-alpine AS builder

WORKDIR /build

# Copy all POMs first → Docker caches dependency download layer
COPY pom.xml .
COPY infrastructure/pom.xml         infrastructure/pom.xml
COPY shared-kernel/pom.xml          shared-kernel/pom.xml
COPY modules/appointment/pom.xml    modules/appointment/pom.xml
COPY modules/child/pom.xml          modules/child/pom.xml
COPY modules/consent/pom.xml        modules/consent/pom.xml
COPY modules/facility/pom.xml       modules/facility/pom.xml
COPY modules/geo/pom.xml            modules/geo/pom.xml
COPY modules/government/pom.xml     modules/government/pom.xml
COPY modules/identity/pom.xml       modules/identity/pom.xml
COPY modules/maternal/pom.xml       modules/maternal/pom.xml
COPY modules/notification/pom.xml   modules/notification/pom.xml
RUN mvn dependency:go-offline -q

# Copy all sources and build the fat-jar (tests run in CI)
COPY src           ./src
COPY infrastructure ./infrastructure
COPY shared-kernel  ./shared-kernel
COPY modules        ./modules
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