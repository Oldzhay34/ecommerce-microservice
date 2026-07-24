package com.gateway.filter;

import com.gateway.dto.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

/**
 * Spring Cloud Gateway'in yerlesik RequestRateLimiter filtresi limit asilinca
 * response.setStatusCode(429) + response.setComplete() cagirir; govde YAZMAZ ve
 * exception firlatmaz, bu yuzden GlobalErrorWebExceptionHandler hic devreye girmez.
 * Bu filtre response'u dekore ederek setComplete() 429 ile (govdesiz) cagrildiginda
 * GlobalErrorWebExceptionHandler ile ayni ErrorResponse govdesini enjekte eder.
 */
@Component
public class RateLimitResponseBodyGlobalFilter implements GlobalFilter, Ordered {

    private final ObjectMapper objectMapper;

    public RateLimitResponseBodyGlobalFilter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpResponse decorated = new ServerHttpResponseDecorator(exchange.getResponse()) {
            @Override
            public Mono<Void> setComplete() {
                if (HttpStatus.TOO_MANY_REQUESTS.equals(getStatusCode()) && !isCommitted()) {
                    return writeErrorBody(this, exchange);
                }
                return super.setComplete();
            }
        };
        return chain.filter(exchange.mutate().response(decorated).build());
    }

    private Mono<Void> writeErrorBody(ServerHttpResponse response, ServerWebExchange exchange) {
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        ErrorResponse body = new ErrorResponse(
                LocalDateTime.now(),
                HttpStatus.TOO_MANY_REQUESTS.value(),
                "Istek limiti asildi. Lutfen biraz sonra tekrar deneyin.",
                exchange.getRequest().getURI().getPath()
        );

        byte[] bytes;
        try {
            bytes = objectMapper.writeValueAsBytes(body);
        } catch (Exception e) {
            bytes = ("{\"status\":429,\"message\":\"Istek limiti asildi.\"}").getBytes(StandardCharsets.UTF_8);
        }

        DataBuffer buffer = response.bufferFactory().wrap(bytes);
        return response.writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        // Route seviyesindeki RequestRateLimiter filtresini (pozitif order) SARMALI:
        // pre-fazda ondan once calisip response'u dekore etmeli.
        return -50;
    }
}
