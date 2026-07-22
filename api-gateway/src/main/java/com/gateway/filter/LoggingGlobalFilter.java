package com.gateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Her istegi loglar: method, path, X-User-Id, response status ve gecen sure (ms).
 * JWT filtresinden SONRA calisir ki X-User-Id header'i cozulmus olsun.
 */
@Component
public class LoggingGlobalFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(LoggingGlobalFilter.class);

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        long startTime = System.currentTimeMillis();
        ServerHttpRequest request = exchange.getRequest();
        String method = request.getMethod().name();
        String path = request.getURI().getPath();
        String userId = request.getHeaders().getFirst("X-User-Id");

        return chain.filter(exchange).then(Mono.fromRunnable(() -> {
            long duration = System.currentTimeMillis() - startTime;
            Integer status = exchange.getResponse().getStatusCode() != null
                    ? exchange.getResponse().getStatusCode().value()
                    : null;
            log.info("[GATEWAY] {} {} | userId={} | status={} | {}ms",
                    method, path, userId != null ? userId : "-", status, duration);
        }));
    }

    @Override
    public int getOrder() {
        // JWT filtresinden (-100) sonra calissin ki header cozulmus olsun.
        return -50;
    }
}
