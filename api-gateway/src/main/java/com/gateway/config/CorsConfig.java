package com.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import org.springframework.web.cors.reactive.CorsWebFilter;

import java.util.List;

@Configuration
public class CorsConfig {

    @Bean
    public CorsWebFilter corsWebFilter() {
        CorsConfiguration config = new CorsConfiguration();

        // 6005 portu eklendi
        config.setAllowedOrigins(List.of(
                "http://localhost:5173",
                "http://localhost:3000",
                "http://localhost:5000",
                "http://localhost:3001",   // ← store shell (host)
                "http://localhost:6001",   // ← mfe-orders
                "http://localhost:6002",   // ← orders dev (vite'ın kaydığı port)
                "http://localhost:6004",   // ← mfe-reviews
                "http://localhost:6005",   // ← mfe-products (zaten var)
                "http://localhost:6006",
                "http://localhost:3002",
                "http://localhost:6008",   // ← EKLENDİ: mfe-media-gallery dev ortamı
                "http://localhost:6010",
                "http://localhost:9000",   // ← MinIO object erişimi (media-service)
                "http://localhost:9001",
                "http://localhost:5004"// ← MinIO Console (dev doğrulama)
        ));

        config.setAllowedMethods(List.of(
                HttpMethod.GET.name(),
                HttpMethod.POST.name(),
                HttpMethod.PUT.name(),
                HttpMethod.PATCH.name(),
                HttpMethod.DELETE.name(),
                HttpMethod.OPTIONS.name()
        ));

        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return new CorsWebFilter(source);
    }
}