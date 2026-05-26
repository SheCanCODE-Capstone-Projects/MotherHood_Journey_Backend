package com.motherhood.journey;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@Testcontainers
public abstract class IntegrationTestBase {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
        .withDatabaseName("motherhood_test")
        .withUsername("test")
        .withPassword("test");

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
