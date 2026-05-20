package com.motherhood.journey.security;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.motherhood.journey.common.dto.ApiResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;

/**
 * Rate-limits POST /api/v1/auth/login to 5 attempts per IP per minute.
 *
 * Uses Bucket4j in-process token buckets, one per client IP.
 * Buckets are stored in a Caffeine cache with 1-hour idle expiry —
 * preventing unbounded memory growth on long-running instances.
 *
 * On Railway, the real client IP is in X-Forwarded-For.
 * Returns HTTP 429 with a standard ApiResponse error body on breach.
 */
@Component
public class LoginRateLimitFilter extends OncePerRequestFilter {

    private static final String LOGIN_PATH           = "/api/v1/auth/login";
    private static final int    MAX_REQUESTS_PER_MIN = 5;

    // Caffeine cache: evicts buckets for IPs idle for more than 1 hour
    // — prevents memory leak on long-running Railway instances.
    private final Cache<String, Bucket> bucketCache = Caffeine.newBuilder()
        .expireAfterAccess(Duration.ofHours(1))
        .maximumSize(10_000)
        .build();

    private final ObjectMapper objectMapper;

    public LoginRateLimitFilter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !("POST".equalsIgnoreCase(request.getMethod())
            && LOGIN_PATH.equals(request.getRequestURI()));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String ip     = resolveClientIp(request);
        Bucket bucket = bucketCache.get(ip, k -> newBucket());

        if (bucket.tryConsume(1)) {
            filterChain.doFilter(request, response);
        } else {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write(
                objectMapper.writeValueAsString(
                    ApiResponse.error("Too many login attempts. Please try again in 1 minute.")));
        }
    }

    private Bucket newBucket() {
        return Bucket.builder()
            .addLimit(Bandwidth.builder()
                .capacity(MAX_REQUESTS_PER_MIN)
                .refillIntervally(MAX_REQUESTS_PER_MIN, Duration.ofMinutes(1))
                .build())
            .build();
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            // Use the LAST entry — appended by the trusted Railway proxy.
            // The first entry is user-controlled and can be spoofed.
            String[] parts = forwarded.split(",");
            return parts[parts.length - 1].trim();
        }
        return request.getRemoteAddr();
    }
}
