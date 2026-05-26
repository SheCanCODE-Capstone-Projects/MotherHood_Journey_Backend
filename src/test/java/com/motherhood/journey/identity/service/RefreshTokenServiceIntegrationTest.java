package com.motherhood.journey.identity.service;

import com.motherhood.journey.IntegrationTestBase;
import com.motherhood.journey.identity.entity.RefreshToken;
import com.motherhood.journey.identity.repository.RefreshTokenRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the refresh_tokens table is created by Flyway and the JPA
 * mapping/repository wiring is correct against a real Postgres.
 */
class RefreshTokenServiceIntegrationTest extends IntegrationTestBase {

    @Autowired RefreshTokenRepository refreshTokenRepository;

    @Test
    void contextLoads_andRefreshTokensTableExists() {
        // baseline: empty table after Flyway runs
        assertThat(refreshTokenRepository.count()).isZero();

        // verify expire-cutoff query parses and executes
        int deleted = refreshTokenRepository.deleteExpired(LocalDateTime.now());
        assertThat(deleted).isZero();
    }

    @Test
    void isUsable_helperBehaviour() {
        RefreshToken usable = RefreshToken.builder()
            .userId(java.util.UUID.randomUUID())
            .tokenHash("h")
            .expiresAt(LocalDateTime.now().plusDays(1))
            .build();
        RefreshToken expired = RefreshToken.builder()
            .userId(java.util.UUID.randomUUID())
            .tokenHash("h2")
            .expiresAt(LocalDateTime.now().minusDays(1))
            .build();
        RefreshToken revoked = RefreshToken.builder()
            .userId(java.util.UUID.randomUUID())
            .tokenHash("h3")
            .expiresAt(LocalDateTime.now().plusDays(1))
            .revokedAt(LocalDateTime.now())
            .build();

        assertThat(usable.isUsable()).isTrue();
        assertThat(expired.isUsable()).isFalse();
        assertThat(revoked.isUsable()).isFalse();
    }
}
