package com.motherhood.journey.identity.repository;

import com.motherhood.journey.geo.entity.GeoLocation;
import com.motherhood.journey.geo.repository.GeoRepository;
import com.motherhood.journey.identity.entity.User;
import com.motherhood.journey.identity.enums.UserRole;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace.NONE;

/**
 * @DataJpaTest slice — Postgres via Testcontainers (NOT H2) so Flyway
 * migrations and Postgres-specific features (JSONB, partitions, gen_random_uuid())
 * exercise the real schema. Validates derived-query naming + Optional return
 * types on UserRepository.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = NONE)
@Testcontainers
class UserRepositoryDataJpaTest {

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
    }

    @Autowired UserRepository userRepository;
    @Autowired GeoRepository geoRepository;

    @Test
    void findByPhoneNumber_returnsEmpty_whenNoUser() {
        assertThat(userRepository.findByPhoneNumber("+250700000000")).isEmpty();
    }

    @Test
    void existsByPhoneNumber_returnsTrue_afterPersist() {
        GeoLocation geo = geoRepository.findAll().stream()
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("V19 geo seed missing"));

        String phone = "+250788" + (int) (Math.random() * 900_000 + 100_000);
        userRepository.save(User.builder()
            .nationalId("1" + System.nanoTime())
            .phoneNumber(phone)
            .passwordHash("$2a$10$placeholder")
            .role(UserRole.PATIENT)
            .firstName("J").lastName("Doe")
            .preferredLanguage("rw")
            .geoLocation(geo)
            .build());

        assertThat(userRepository.existsByPhoneNumber(phone)).isTrue();
        assertThat(userRepository.findByPhoneNumber(phone)).isPresent();
    }

    @Test
    void findByRole_returnsOnlyMatchingRole() {
        GeoLocation geo = geoRepository.findAll().stream().findFirst().orElseThrow();

        long govBefore = userRepository.findByRole(UserRole.MOH_ADMIN).size();

        userRepository.save(User.builder()
            .nationalId("1" + System.nanoTime())
            .phoneNumber("+250789" + (int) (Math.random() * 900_000 + 100_000))
            .passwordHash("$2a$10$placeholder")
            .role(UserRole.MOH_ADMIN)
            .firstName("M").lastName("OH")
            .preferredLanguage("en")
            .geoLocation(geo)
            .build());

        assertThat(userRepository.findByRole(UserRole.MOH_ADMIN)).hasSize((int) govBefore + 1);
        assertThat(userRepository.findByRole(UserRole.MOH_ADMIN))
            .allMatch(u -> u.getRole() == UserRole.MOH_ADMIN);
    }
}
