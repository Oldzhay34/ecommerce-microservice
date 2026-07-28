package com.mediaservice.infrastructure.config;

import com.mediaservice.infrastructure.security.JwtAuthFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Product servisinin SecurityConfig'i ile ayni desen (@EnableWebSecurity +
 * @EnableMethodSecurity(prePostEnabled = true), STATELESS, csrf disable, filter registration).
 *
 * Fark: Media servisinde IKI public endpoint vardir (Product'ta yalnizca /products/search).
 * Product detay ve liste ekrani token'siz calistigi icin gorsel okuma acik olmalidir.
 *
 * CORS Gateway'de yonetilir (CorsConfig); burada kapali.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true) // @PreAuthorize anotasyonlari icin gerekli
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Public: urun detay/liste ekrani token'siz calisir
                        .requestMatchers(HttpMethod.GET, "/api/v1/media/products/*/images").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/media/products/images/batch").permitAll()
                        .requestMatchers("/actuator/health", "/actuator/info", "/actuator/prometheus", "/actuator/metrics").permitAll()
                        // Korumali: rol bazli
                        .requestMatchers(HttpMethod.POST, "/api/v1/media/products/*/images")
                        .hasRole("STORE")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/media/images/*")
                        .hasAnyRole("STORE", "ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/media/images/*/primary")
                        .hasAnyRole("STORE", "ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/media/products/*/images/order")
                        .hasAnyRole("STORE", "ADMIN")
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}