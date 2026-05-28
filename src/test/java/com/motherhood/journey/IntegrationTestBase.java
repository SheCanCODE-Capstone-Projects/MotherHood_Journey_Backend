package com.motherhood.journey;

import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Singleton-container base class: POSTGRES is started once per JVM via the static initializer,
 * not per test class. This keeps the JDBC URL stable so Spring's test-context cache can reuse
 * one ApplicationContext across all subclasses instead of creating a new one per class (which
 * would point at a stopped container).
 */
@SpringBootTest
@ExtendWith(DockerAvailableCondition.class)
public abstract class IntegrationTestBase {

    static final PostgreSQLContainer<?> POSTGRES;

    static {
        POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("motherhood_test")
            .withUsername("test")
            .withPassword("test");
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("jwt.secret", () ->
            "dGVzdC1qd3Qtc2VjcmV0LWtleS1mb3ItaW50ZWdyYXRpb24tdGVzdGluZy1vbmx5LW5vdC1mb3ItcHJvZA");
        registry.add("africas-talking.api-key", () -> "test-key");
        registry.add("africas-talking.username", () -> "test-user");
    }
}
