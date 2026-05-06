package com.motherhood.journey.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfigurationSource;
import com.motherhood.journey.identity.enums.UserRole;

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
                                UserRole.HEALTH_WORKER.name(),
                                UserRole.FACILITY_ADMIN.name()
                        )
                        .requestMatchers("/api/v1/children/**")
                        .hasAnyRole(
                                UserRole.HEALTH_WORKER.name(),
                                UserRole.FACILITY_ADMIN.name()
                        )
                        .requestMatchers("/api/v1/appointments/**")
                        .hasAnyRole(
                                UserRole.HEALTH_WORKER.name(),
                                UserRole.FACILITY_ADMIN.name(),
                                UserRole.PATIENT.name()
                        )
                        .requestMatchers("/api/v1/reports/**")
                        .hasAnyRole(
                                UserRole.GOVERNMENT_ANALYST.name(),
                                UserRole.MOH_ADMIN.name()
                        )
                        .requestMatchers("/api/v1/facilities/**")
                        .hasAnyRole(
                                UserRole.FACILITY_ADMIN.name(),
                                UserRole.MOH_ADMIN.name()
                        )
                        .anyRequest().authenticated()
                );


        return http.build();
    }
}