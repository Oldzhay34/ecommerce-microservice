package com.review.infrastructure.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // BUGFIX: Bir handler exception fırlattığında servlet
                        // container isteği ERROR dispatch ile /error'a yönlendirir.
                        // JwtAuthFilter bir OncePerRequestFilter olduğu için ERROR
                        // dispatch'te ÇALIŞMAZ (shouldNotFilterErrorDispatch=true),
                        // dolayısıyla SecurityContext o noktada boştur. "/error"
                        // permitAll olmadığı sürece anyRequest().authenticated()
                        // kuralı devreye girer ve HER 5xx istemciye 403 olarak
                        // döner - asıl hata tamamen gizlenir.
                        .requestMatchers("/error").permitAll()
                        // Prometheus JWT ile authenticate olamaz; scrape endpoint'i public olmali.
                        .requestMatchers("/actuator/health", "/actuator/prometheus", "/actuator/metrics").permitAll()
                        .requestMatchers("/api/internal/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/reviews/product/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/reviews").hasAuthority("ROLE_CUSTOMER")
                        .requestMatchers(HttpMethod.GET, "/api/reviews/me").hasAuthority("ROLE_CUSTOMER")
                        .requestMatchers(HttpMethod.PATCH, "/api/reviews/*/reply").hasAuthority("ROLE_STORE")
                        .requestMatchers(HttpMethod.PATCH, "/api/reviews/*/moderate").hasAuthority("ROLE_ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/reviews/all").hasAuthority("ROLE_ADMIN")
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}