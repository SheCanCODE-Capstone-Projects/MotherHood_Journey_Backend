package com.motherhood.journey.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import javax.crypto.SecretKey;

@Component
public class JwtUtil {

    private static final Logger log = LoggerFactory.getLogger(JwtUtil.class);

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration-ms:86400000}")
    private long expirationMs;

    @PostConstruct
    void validateSecret() {
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException(
                "jwt.secret must be at least 32 characters (256 bits). " +
                "Set a strong value in your environment variables.");
        }
    }

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(String phoneNumber, String role, UUID facilityId) {
        return Jwts.builder()
            .subject(phoneNumber)
            .claim("role", role)
            .claim("facilityId", facilityId != null ? facilityId.toString() : null)
            .issuedAt(new Date())
            .expiration(new Date(System.currentTimeMillis() + expirationMs))
            .signWith(getSigningKey())
            .compact();
    }

    public String extractPhoneNumber(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public UUID extractFacilityId(String token) {
        String raw = extractClaim(token, claims -> claims.get("facilityId", String.class));
        if (raw == null) {
            return null;
        }
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public String extractRole(String token) {
        return extractClaim(token, claims -> claims.get("role", String.class));
    }

    @SuppressWarnings("unchecked")
    public List<UUID> extractGeoScopeIds(String token) {
        Object raw = extractClaim(token, claims -> claims.get(JwtClaims.GEO_SCOPE_IDS));
        if (raw == null) return List.of();
        return ((List<String>) raw).stream().map(UUID::fromString).toList();
    }

    public boolean isTokenValid(String token) {
        try {
            return !extractClaim(token, Claims::getExpiration).before(new Date());
        } catch (ExpiredJwtException e) {
            log.debug("JWT expired");
            return false;
        } catch (JwtException e) {
            log.warn("JWT validation failed: {}", e.getMessage());
            return false;
        }
    }

    private <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        Claims claims = Jwts.parser()
            .verifyWith(getSigningKey())
            .build()
            .parseSignedClaims(token)
            .getPayload();
        return claimsResolver.apply(claims);
    }
}
