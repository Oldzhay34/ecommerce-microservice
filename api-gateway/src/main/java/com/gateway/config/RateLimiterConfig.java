package com.gateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

/**
 * Redis tabanli RequestRateLimiter icin KeyResolver.
 *
 * Oncelik: X-User-Id header'i (JwtAuthenticationGlobalFilter tarafindan set edilir).
 * Yoksa (public route veya kimliksiz istek) client IP adresine gore limitle.
 *
 * Rate limiter parametreleri (replenishRate/burstCapacity) application.yml icinde
 * her route'un RequestRateLimiter filtresinde tanimlidir.
 */
@Configuration
public class RateLimiterConfig {

    @Bean
    public KeyResolver userKeyResolver() {
        return exchange -> {
            String userId = exchange.getRequest().getHeaders().getFirst("X-User-Id");
            if (userId != null && !userId.isBlank()) {
                return Mono.just(userId);
            }
            String ip = exchange.getRequest().getRemoteAddress() != null
                    ? exchange.getRequest().getRemoteAddress().getAddress().getHostAddress()
                    : "unknown";
            return Mono.just(ip);
        };
    }
}
