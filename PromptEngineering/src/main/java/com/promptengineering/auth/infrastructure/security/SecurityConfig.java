package com.promptengineering.auth.infrastructure.security;

import com.promptengineering.auth.infrastructure.security.filter.JwtAuthFilter;
import com.promptengineering.auth.infrastructure.security.filter.RateLimitFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final RateLimitFilter rateLimitFilter;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter, RateLimitFilter rateLimitFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
        this.rateLimitFilter = rateLimitFilter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(AbstractHttpConfigurer::disable) // CORS'u kapattık, Gateway halledecek
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/v1/auth/register", "/api/v1/auth/verify-otp", "/api/v1/auth/login").permitAll()
                        // Prometheus JWT ile authenticate olamaz; scrape endpoint'i public olmali.
                        .requestMatchers("/actuator/health", "/actuator/info", "/actuator/prometheus", "/actuator/metrics").permitAll()
                        // OPTIONS isteklerine global izin vermek Spring Security'de önemlidir
                        .requestMatchers(org.springframework.http.HttpMethod.OPTIONS, "/**").permitAll()
                        .anyRequest().authenticated()
                )
                // JWT filtresi standart authentication'dan önce çalışmalı
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                // Rate limit, SecurityContext oluştuktan sonra çalışmalı ki rolleri okuyabilsin
                .addFilterAfter(rateLimitFilter, JwtAuthFilter.class);

        return http.build();
    }

}