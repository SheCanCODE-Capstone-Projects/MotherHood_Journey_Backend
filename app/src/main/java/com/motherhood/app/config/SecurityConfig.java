package com.motherhood.app.config;

import com.motherhood.identity.domain.enums.Role;
import com.motherhood.identity.infrastructure.security.JwtAuthFilter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final CorsConfigurationSource corsConfigurationSource;
    private final JwtAuthFilter jwtAuthFilter;

    public SecurityConfig(
            @Qualifier("corsConfigurationSource") CorsConfigurationSource corsConfigurationSource,
            JwtAuthFilter jwtAuthFilter
    ) {
        this.corsConfigurationSource = corsConfigurationSource;
        this.jwtAuthFilter = jwtAuthFilter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
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
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        .requestMatchers("/api/v1/mothers/**")
                            .hasAnyRole(Role.HEALTH_WORKER.name(), Role.FACILITY_ADMIN.name(), Role.DISTRICT_OFFICER.name())
                        .requestMatchers("/api/v1/children/**")
                            .hasAnyRole(Role.HEALTH_WORKER.name(), Role.FACILITY_ADMIN.name())
                        .requestMatchers("/api/v1/appointments/**")
                            .hasAnyRole(Role.HEALTH_WORKER.name(), Role.FACILITY_ADMIN.name(), Role.PATIENT.name())
                        .requestMatchers("/api/v1/reports/**")
                            .hasAnyRole(Role.GOVERNMENT_ANALYST.name(), Role.MOH_ADMIN.name())
                        .requestMatchers("/api/v1/facilities/**")
                            .hasAnyRole(Role.FACILITY_ADMIN.name(), Role.MOH_ADMIN.name())
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
