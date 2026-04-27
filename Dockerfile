# Stage 1 — Build the application using Maven
FROM maven:3.9.6-eclipse-temurin-21 AS builder

# Set working directory inside the container
WORKDIR /app

# Copy parent pom.xml first
COPY pom.xml .

# Copy all module pom.xml files
COPY app/pom.xml app/
COPY shared-kernel/pom.xml shared-kernel/
COPY infrastructure/pom.xml infrastructure/
COPY modules/geo/pom.xml modules/geo/
COPY modules/identity/pom.xml modules/identity/
COPY modules/facility/pom.xml modules/facility/
COPY modules/maternal/pom.xml modules/maternal/
COPY modules/child/pom.xml modules/child/
COPY modules/appointment/pom.xml modules/appointment/
COPY modules/consent/pom.xml modules/consent/
COPY modules/notification/pom.xml modules/notification/
COPY modules/government/pom.xml modules/government/

# Download all dependencies first
# This layer is cached — only re-runs if pom.xml files change
RUN mvn dependency:go-offline -B

# Copy all source code
COPY app/src app/src
COPY shared-kernel/src shared-kernel/src
COPY infrastructure/src infrastructure/src
COPY modules/geo/src modules/geo/src
COPY modules/identity/src modules/identity/src
COPY modules/facility/src modules/facility/src
COPY modules/maternal/src modules/maternal/src
COPY modules/child/src modules/child/src
COPY modules/appointment/src modules/appointment/src
COPY modules/consent/src modules/consent/src
COPY modules/notification/src modules/notification/src
COPY modules/government/src modules/government/src

# Build the jar — skip tests because CI already ran them
RUN mvn package -DskipTests -B

# Stage 2 — Run the application using only Java
FROM eclipse-temurin:21-jre-alpine

# Set working directory
WORKDIR /app

# Copy the built jar from Stage 1
COPY --from=builder /app/app/target/*.jar app.jar

# Expose the port Spring Boot runs on
EXPOSE 8080

# Start the application
ENTRYPOINT ["java", "-jar", "app.jar"]