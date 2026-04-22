
package com.motherhood.identity.infrastructure.security;

import com.motherhood.identity.domain.entity.User;
import com.motherhood.identity.domain.repository.UserRepository;
import com.motherhood.identity.domain.service.AuditLogService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";
    private static final String RESOURCE_TYPE = "jwt_auth";

    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String token = extractToken(request);

        if (token == null) {
            filterChain.doFilter(request, response);
            return;
        }

        if (!jwtTokenProvider.validateToken(token)) {
            logFailure(null, "Invalid or expired token", request);
            filterChain.doFilter(request, response);
            return;
        }

        try {
            String phoneNumber = jwtTokenProvider.getPhoneNumber(token);
            UUID userId = jwtTokenProvider.getUserId(token);

            User user = userRepository.findByPhoneNumber(phoneNumber)
                    .orElse(null);

            if (user == null || !user.isActive()) {
                logFailure(userId, "User not found or inactive", request);
                filterChain.doFilter(request, response);
                return;
            }

            request.setAttribute("userId", userId);
            request.setAttribute("facilityId", jwtTokenProvider.getFacilityId(token));
            request.setAttribute("geoScopeIds", jwtTokenProvider.getGeoScopeIds(token));

            UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(
                            user,
                            null,
                            List.of(new SimpleGrantedAuthority(user.getRole().name()))
                    );

            auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(auth);

        } catch (Exception e) {
            logFailure(null, "Token processing error", request);
        }

        filterChain.doFilter(request, response);
    }

    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (StringUtils.hasText(header) && header.startsWith(BEARER_PREFIX)) {
            return header.substring(BEARER_PREFIX.length());
        }
        return null;
    }

    private void logFailure(UUID userId, String reason, HttpServletRequest request) {
        auditLogService.logFailure(
                userId,
                "AUTH",
                RESOURCE_TYPE,
                reason,
                request.getRemoteAddr(),
                request.getHeader(HttpHeaders.USER_AGENT)
        );
    }
}