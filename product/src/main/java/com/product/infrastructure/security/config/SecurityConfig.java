package com.product.infrastructure.security.config;

import com.product.infrastructure.security.filter.JwtResourceFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true) // @PreAuthorize anotasyonları için gerekli
public class SecurityConfig {

    private final JwtResourceFilter jwtResourceFilter;

    public SecurityConfig(JwtResourceFilter jwtResourceFilter) {
        this.jwtResourceFilter = jwtResourceFilter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/v1/products/search").permitAll()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtResourceFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}