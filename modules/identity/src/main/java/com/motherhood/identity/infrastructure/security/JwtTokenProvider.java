package com.motherhood.identity.infrastructure.security;

import com.motherhood.identity.domain.entity.User;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SecurityException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import javax.crypto.SecretKey;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
public class JwtTokenProvider {

    private final SecretKey signingKey;
    private final long expirationMs;
    private final long refreshExpirationMs;

    public JwtTokenProvider(
            @Value("${jwt.secret}") String base64Secret,
            @Value("${jwt.expiration-ms:86400000}") long expirationMs,
            @Value("${jwt.refresh-expiration-ms:604800000}") long refreshExpirationMs) {

        this.signingKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(base64Secret));
        this.expirationMs = expirationMs;
        this.refreshExpirationMs = refreshExpirationMs;
    }


    public String generateAccessToken(User user) {
        return buildToken(user, expirationMs);
    }

    public String generateRefreshToken(User user) {
        return buildToken(user, refreshExpirationMs);
    }

    private String buildToken(User user, long ttlMs) {
        Date now    = new Date();
        Date expiry = new Date(now.getTime() + ttlMs);

        JwtBuilder builder = Jwts.builder()
                .subject(user.getId().toString())
                .issuedAt(now)
                .expiration(expiry)
                .claim(JwtClaims.USER_ID,  user.getId().toString())
                .claim(JwtClaims.PHONE,    user.getPhoneNumber())
                .claim(JwtClaims.ROLE,     user.getRole().name())
                .claim(JwtClaims.LANGUAGE, user.getPreferredLanguage());

        if (user.getFacilityId() != null) {
            builder.claim(JwtClaims.FACILITY_ID, user.getFacilityId().toString());
        }

        if (user.getRole().isGovernmentRole() && user.getScopedGeoIds() != null) {
            builder.claim(JwtClaims.GEO_SCOPE_IDS,
                    user.getScopedGeoIds().stream().map(UUID::toString).toList());
        }

        return builder.signWith(signingKey, Jwts.SIG.HS256).compact();
    }


    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (ExpiredJwtException e)
        { log.warn("JWT expired: {}", e.getMessage()); }
        catch (UnsupportedJwtException e)
        { log.warn("Unsupported JWT: {}", e.getMessage()); }
        catch (MalformedJwtException e)
        { log.warn("Malformed JWT: {}", e.getMessage()); }
        catch (SecurityException e)
        { log.warn("Invalid JWT signature: {}", e.getMessage()); }
        catch (IllegalArgumentException e) { log.warn("Empty JWT claims: {}", e.getMessage()); }
        return false;
    }



    public String getPhoneNumber(String token) {
        return (String) parseClaims(token).get(JwtClaims.PHONE);
    }

    public UUID getUserId(String token) {
        return UUID.fromString((String) parseClaims(token).get(JwtClaims.USER_ID));
    }

    public String getRole(String token) {
        return (String) parseClaims(token).get(JwtClaims.ROLE);
    }

    public UUID getFacilityId(String token) {
        String fid = (String) parseClaims(token).get(JwtClaims.FACILITY_ID);
        return fid != null ? UUID.fromString(fid) : null;
    }

    @SuppressWarnings("unchecked")
    public List<UUID> getGeoScopeIds(String token) {
        Object raw = parseClaims(token).get(JwtClaims.GEO_SCOPE_IDS);
        if (raw == null) return List.of();
        return ((List<String>) raw).stream().map(UUID::fromString).toList();
    }

    public boolean isTokenExpired(String token) {
        try {
            return parseClaims(token).getExpiration().before(new Date());
        } catch (ExpiredJwtException e) {
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            // treat invalid tokens as "not usable"; caller should pair with validateToken
            return true;
        }
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}