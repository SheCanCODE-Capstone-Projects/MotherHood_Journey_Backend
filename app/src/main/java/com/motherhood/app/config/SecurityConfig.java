package com.motherhood.app.config;

import com.motherhood.shared.rbac.Role;
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
                // Disable CSRF — not needed for stateless REST APIs using JWT
                .csrf(AbstractHttpConfigurer::disable)

                // Apply our CORS config from CorsConfig.java
                .cors(cors -> cors.configurationSource(corsConfigurationSource))

                // Stateless session — no HTTP session, JWT handles auth
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // URL access rules
                .authorizeHttpRequests(auth -> auth

                        // Geo endpoints — public, no login needed (dropdown menus)
                        .requestMatchers("/api/v1/geo/**").permitAll()

                        // Auth endpoints — public
                        .requestMatchers("/api/v1/auth/**").permitAll()

                        // Swagger UI — public
                        .requestMatchers(
                                "/swagger-ui/**",
                                "/v3/api-docs/**"
                        ).permitAll()

                        // Mothers records — health workers and facility admins only
                        .requestMatchers("/api/v1/mothers/**")
                        .hasAnyRole(
                                Role.HEALTH_WORKER.name(),
                                Role.FACILITY_ADMIN.name()
                        )

                        // Child records — same as mothers
                        .requestMatchers("/api/v1/children/**")
                        .hasAnyRole(
                                Role.HEALTH_WORKER.name(),
                                Role.FACILITY_ADMIN.name()
                        )

                        // Appointments
                        .requestMatchers("/api/v1/appointments/**")
                        .hasAnyRole(
                                Role.HEALTH_WORKER.name(),
                                Role.FACILITY_ADMIN.name(),
                                Role.PATIENT.name()
                        )

                        // Government reports — analysts and MoH admins only
                        .requestMatchers("/api/v1/reports/**")
                        .hasAnyRole(
                                Role.GOVERNMENT_ANALYST.name(),
                                Role.MOH_ADMIN.name()
                        )

                        // Facility management
                        .requestMatchers("/api/v1/facilities/**")
                        .hasAnyRole(
                                Role.FACILITY_ADMIN.name(),
                                Role.MOH_ADMIN.name()
                        )

                        // Everything else requires authentication
                        .anyRequest().authenticated()
                );

        // TODO: wire JwtAuthFilter here once teammate merges issue #4
        // .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)

        return http.build();
    }


    }
