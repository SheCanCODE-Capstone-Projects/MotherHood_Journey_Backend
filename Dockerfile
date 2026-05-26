# ── Stage 1: Build ────────────────────────────────────────────────
FROM maven:3.9.6-eclipse-temurin-21-alpine AS builder
WORKDIR /build

COPY pom.xml .
RUN mvn dependency:go-offline -q

COPY src ./src
RUN mvn package -DskipTests -q

# ── Stage 2: Extract layered jar ──────────────────────────────────
# Spring Boot layered jars split the fat-jar into four layers ordered
# by change frequency. Docker only rebuilds layers that change, so
# rebuilding after a code change only touches the thin "application" layer.
FROM eclipse-temurin:21-jre-alpine AS extractor
WORKDIR /app
COPY --from=builder /build/target/motherhood-journey-*.jar app.jar
RUN java -Djarmode=layertools -jar app.jar extract --destination extracted

# ── Stage 3: Runtime ──────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine AS runtime

RUN addgroup -S appgroup && adduser -S appuser -G appgroup

WORKDIR /app

# Copy layers least-to-most volatile → best layer cache reuse
COPY --from=extractor --chown=appuser:appgroup /app/extracted/dependencies/          ./
COPY --from=extractor --chown=appuser:appgroup /app/extracted/spring-boot-loader/    ./
COPY --from=extractor --chown=appuser:appgroup /app/extracted/snapshot-dependencies/ ./
COPY --from=extractor --chown=appuser:appgroup /app/extracted/application/           ./

USER appuser

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=10s --start-period=90s --retries=3 \
  CMD wget -qO- http://localhost:8080/actuator/health || exit 1

# Exec-form (no shell wrapper) ensures SIGTERM reaches the JVM for graceful shutdown.
ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-XX:InitialRAMPercentage=50.0", \
  "-XX:+UseG1GC", \
  "-XX:MaxGCPauseMillis=200", \
  "-XX:+UseStringDeduplication", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-Dfile.encoding=UTF-8", \
  "-Dspring.profiles.active=prod", \
  "org.springframework.boot.loader.launch.JarLauncher"]
