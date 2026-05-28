package com.motherhood.journey.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
public class CorsConfig {

    /**
     * Additional exact origins injected from CORS_ALLOWED_ORIGINS (comma-separated).
     * Used for production domains. Localhost is always allowed via origin patterns below.
     */
    @Value("${app.cors.allowed-origins:}")
    private List<String> extraAllowedOrigins;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        // Allow any localhost / 127.0.0.1 port — covers React (:3000), Angular (:4200),
        // Vite (:5173), or any other local dev server without needing to enumerate ports.
        config.setAllowedOriginPatterns(List.of(
            "http://localhost:*",
            "http://127.0.0.1:*"
        ));

        // Merge production origins from env (e.g. https://app.motherhood.rw)
        if (extraAllowedOrigins != null) {
            extraAllowedOrigins.stream()
                .filter(o -> o != null && !o.isBlank())
                .forEach(config::addAllowedOrigin);
        }

        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept"));
        config.setExposedHeaders(List.of("Authorization"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        source.registerCorsConfiguration("/webhooks/**", config);
        return source;
    }
}
