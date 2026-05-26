package com.motherhood.journey.identity.service;

import com.motherhood.journey.common.exception.CustomException;
import com.motherhood.journey.identity.entity.RefreshToken;
import com.motherhood.journey.identity.entity.User;
import com.motherhood.journey.identity.repository.RefreshTokenRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

@Service
public class RefreshTokenService {

    private static final int TOKEN_BYTES = 48;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final RefreshTokenRepository repository;

    @Value("${jwt.refresh-expiration-ms:604800000}")
    private long refreshTtlMs;

    public RefreshTokenService(RefreshTokenRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public IssuedToken issue(User user, String ipAddress, String userAgent) {
        String raw = generateRaw();
        String hash = sha256(raw);

        RefreshToken entity = RefreshToken.builder()
            .userId(user.getId())
            .tokenHash(hash)
            .expiresAt(LocalDateTime.now().plusNanos(refreshTtlMs * 1_000_000L))
            .ipAddress(ipAddress)
            .userAgent(userAgent)
            .build();

        repository.save(entity);
        return new IssuedToken(raw, entity.getExpiresAt());
    }

    /**
     * Validates a refresh token, revokes it (rotation), issues a new one,
     * and returns it. Throws if the token is unknown, expired, revoked,
     * or if reuse of an already-rotated token is detected (token theft signal).
     */
    @Transactional
    public RotatedToken rotate(String rawToken, String ipAddress, String userAgent) {
        String hash = sha256(rawToken);
        RefreshToken existing = repository.findByTokenHash(hash)
            .orElseThrow(() -> new CustomException("Invalid refresh token", HttpStatus.UNAUTHORIZED));

        if (existing.getRevokedAt() != null) {
            // Reuse of a revoked token — possible theft. Revoke entire family.
            repository.revokeAllForUser(existing.getUserId(), LocalDateTime.now());
            throw new CustomException("Refresh token reuse detected", HttpStatus.UNAUTHORIZED);
        }

        if (existing.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new CustomException("Refresh token expired", HttpStatus.UNAUTHORIZED);
        }

        String newRaw = generateRaw();
        String newHash = sha256(newRaw);

        existing.setRevokedAt(LocalDateTime.now());
        existing.setReplacedByTokenHash(newHash);

        RefreshToken next = RefreshToken.builder()
            .userId(existing.getUserId())
            .tokenHash(newHash)
            .expiresAt(LocalDateTime.now().plusNanos(refreshTtlMs * 1_000_000L))
            .ipAddress(ipAddress)
            .userAgent(userAgent)
            .build();
        repository.save(next);

        return new RotatedToken(existing.getUserId(), newRaw, next.getExpiresAt());
    }

    @Transactional
    public void revoke(String rawToken) {
        repository.findByTokenHash(sha256(rawToken)).ifPresent(t -> {
            if (t.getRevokedAt() == null) {
                t.setRevokedAt(LocalDateTime.now());
            }
        });
    }

    @Transactional
    public int revokeAllForUser(UUID userId) {
        return repository.revokeAllForUser(userId, LocalDateTime.now());
    }

    private String generateRaw() {
        byte[] bytes = new byte[TOKEN_BYTES];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String sha256(String value) {
        try {
            return HexFormat.of().formatHex(
                MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    public record IssuedToken(String rawToken, LocalDateTime expiresAt) {}

    public record RotatedToken(UUID userId, String rawToken, LocalDateTime expiresAt) {}
}
