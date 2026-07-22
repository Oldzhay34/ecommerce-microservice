package com.payment.infrastructure.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // BUG DÜZELTMESİ: /error permitAll değildi. Servlet konteyneri
                        // bir 5xx'i /error'a ERROR dispatch ile yönlendirir; JwtAuthFilter
                        // bir OncePerRequestFilter olduğu için ERROR dispatch'te YENİDEN
                        // ÇALIŞMAZ ve SecurityContext o noktada boştur. Sonuç: gerçek 500
                        // hataları istemciye 403 olarak dönüyor, hata gövdesi kayboluyordu.
                        .requestMatchers("/error").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/payments/me", "/api/payments/me/**").hasRole("CUSTOMER")
                        .requestMatchers(HttpMethod.GET, "/api/payments").hasAnyRole("STORE", "ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/payments/*/refund-approve").hasRole("ADMIN")
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}