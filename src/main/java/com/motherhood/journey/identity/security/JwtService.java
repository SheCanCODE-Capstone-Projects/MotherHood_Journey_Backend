package com.motherhood.journey.identity.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtService {

    private final SecretKey key;
    private final long expirationMs;
    private final long refreshExpirationMs;

    public JwtService(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration-ms}") long expirationMs,
            @Value("${jwt.refresh-expiration-ms}") long refreshExpirationMs
    ) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
        this.refreshExpirationMs = refreshExpirationMs;
    }

    public String generateAccessToken(String subject, String role) {
        return buildToken(subject, role, expirationMs, "access");
    }

    public String generateRefreshToken(String subject, String role) {
        return buildToken(subject, role, refreshExpirationMs, "refresh");
    }

    private String buildToken(String subject, String role, long ttlMs, String tokenType) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .subject(subject)
                .claim("role", role)
                .claim("type", tokenType)
                .issuedAt(new Date(now))
                .expiration(new Date(now + ttlMs))
                .signWith(key)
                .compact();
    }

    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /** Returns the subject (userId) if the token is valid and of the expected type. */
    public String validateAndGetSubject(String token, String expectedType) {
        try {
            Claims claims = parseToken(token);
            if (!expectedType.equals(claims.get("type", String.class))) {
                throw new JwtException("Wrong token type");
            }
            return claims.getSubject();
        } catch (JwtException | IllegalArgumentException e) {
            throw new JwtException("Invalid or expired token: " + e.getMessage(), e);
        }
    }

    public String getRoleClaim(String token) {
        return parseToken(token).get("role", String.class);
    }
}
