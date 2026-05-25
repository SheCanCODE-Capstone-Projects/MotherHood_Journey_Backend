package com.motherhood.journey.config;

import com.motherhood.journey.identity.enums.UserRole;
import com.motherhood.journey.security.JwtFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
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
@EnableMethodSecurity
public class SecurityConfig {

    private final CorsConfigurationSource corsConfigurationSource;
    private final JwtFilter jwtFilter;

    public SecurityConfig(CorsConfigurationSource corsConfigurationSource,
                          JwtFilter jwtFilter) {
        this.corsConfigurationSource = corsConfigurationSource;
        this.jwtFilter = jwtFilter;
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
                        .requestMatchers("/webhooks/at/**").permitAll()
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        .requestMatchers("/actuator/health").permitAll()
                        .requestMatchers("/api/v1/mothers/**")
                            .hasAnyRole(UserRole.HEALTH_WORKER.name(), UserRole.FACILITY_ADMIN.name())
                        .requestMatchers("/api/v1/children/**")
                            .hasAnyRole(UserRole.HEALTH_WORKER.name(), UserRole.FACILITY_ADMIN.name())
                        .requestMatchers("/api/v1/appointments/**")
                            .hasAnyRole(UserRole.HEALTH_WORKER.name(), UserRole.FACILITY_ADMIN.name(), UserRole.PATIENT.name())
                        .requestMatchers("/api/v1/reports/**")
                            .hasAnyRole(UserRole.GOVERNMENT_ANALYST.name(), UserRole.MOH_ADMIN.name())
                        .requestMatchers("/api/v1/facilities/**")
                            .hasAnyRole(UserRole.FACILITY_ADMIN.name(), UserRole.MOH_ADMIN.name())
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}