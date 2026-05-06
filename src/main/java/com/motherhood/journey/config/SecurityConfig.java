package com.motherhood.journey.config;

import com.motherhood.journey.security.rbac.Role;  // ← update this once you know the exact path
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfigurationSource;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final CorsConfigurationSource corsConfigurationSource;

    public SecurityConfig(CorsConfigurationSource corsConfigurationSource) {
        this.corsConfigurationSource = corsConfigurationSource;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/v1/geo/**").permitAll()
                        .requestMatchers("/api/v1/auth/**").permitAll()
                        .requestMatchers(
                                "/swagger-ui/**",
                                "/v3/api-docs/**"
                        ).permitAll()
                        .requestMatchers("/api/v1/mothers/**")
                        .hasAnyRole(
                                Role.HEALTH_WORKER.name(),
                                Role.FACILITY_ADMIN.name()
                        )
                        .requestMatchers("/api/v1/children/**")
                        .hasAnyRole(
                                Role.HEALTH_WORKER.name(),
                                Role.FACILITY_ADMIN.name()
                        )
                        .requestMatchers("/api/v1/appointments/**")
                        .hasAnyRole(
                                Role.HEALTH_WORKER.name(),
                                Role.FACILITY_ADMIN.name(),
                                Role.PATIENT.name()
                        )
                        .requestMatchers("/api/v1/reports/**")
                        .hasAnyRole(
                                Role.GOVERNMENT_ANALYST.name(),
                                Role.MOH_ADMIN.name()
                        )
                        .requestMatchers("/api/v1/facilities/**")
                        .hasAnyRole(
                                Role.FACILITY_ADMIN.name(),
                                Role.MOH_ADMIN.name()
                        )
                        .anyRequest().authenticated()
                );


        return http.build();
    }
}