package com.gateway.filter;

import com.gateway.dto.ErrorResponse;
import com.gateway.security.JwtValidator;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Thin Edge Validation:
 * - Public route'lari (auth, product/search, media okuma) atlar.
 * - Diger route'larda Authorization: Bearer <token> zorunlu; dogrular.
 * - Basarisizsa istek downstream'e gitmeden 401 doner (fail-fast).
 * - Basariliysa: client'in gonderdigi sahte X-User-Id/X-User-Role header'lari
 *   TEMIZLENIR, gateway'in cozdugu degerlerle set edilir. Authorization AYNEN forward edilir.
 */
@Component
public class JwtAuthenticationGlobalFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationGlobalFilter.class);

    private static final String BEARER_PREFIX = "Bearer ";
    private static final String HEADER_USER_ID = "X-User-Id";
    private static final String HEADER_USER_ROLE = "X-User-Role";

    // JWT'den muaf public path'ler (tam eslesme veya prefix)
    private static final List<String> PUBLIC_PATH_PREFIXES = List.of("/api/v1/auth/");
    private static final List<String> PUBLIC_PATH_EXACT = List.of("/api/v1/products/search");

    /**
     * Method + path pattern ile tanimlanan public endpoint'ler.
     *
     * NEDEN AYRI LISTE: Media servisinde ayni path iki farkli yetki seviyesine sahiptir.
     *   GET  /api/v1/media/products/{id}/images  -> public (urun detay ekrani token'siz calisir)
     *   POST /api/v1/media/products/{id}/images  -> ROLE_STORE (gorsel yukleme)
     * Yalnizca path'e bakan bir whitelist upload'i da token'siz gecirirdi.
     */
    private static final List<PublicEndpoint> PUBLIC_ENDPOINTS = List.of(
            new PublicEndpoint(HttpMethod.GET, "/api/v1/media/products/*/images"),
            new PublicEndpoint(HttpMethod.POST, "/api/v1/media/products/images/batch"),
            new PublicEndpoint(HttpMethod.GET, "/api/reviews/product/*")
    );

    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    private final JwtValidator jwtValidator;
    private final ObjectMapper objectMapper;

    public JwtAuthenticationGlobalFilter(JwtValidator jwtValidator, ObjectMapper objectMapper) {
        this.jwtValidator = jwtValidator;
        this.objectMapper = objectMapper;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        if (isPublic(request)) {
            // Public route bile olsa, client'in sahte kimlik header'i enjekte etmesini engelle.
            ServerHttpRequest sanitized = request.mutate()
                    .headers(h -> {
                        h.remove(HEADER_USER_ID);
                        h.remove(HEADER_USER_ROLE);
                    })
                    .build();
            return chain.filter(exchange.mutate().request(sanitized).build());
        }

        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            return unauthorized(exchange, "Authorization header eksik veya Bearer formatinda degil.");
        }

        String token = authHeader.substring(BEARER_PREFIX.length()).trim();

        final Claims claims;
        try {
            claims = jwtValidator.validateAndGetClaims(token);
        } catch (Exception ex) {
            log.warn("JWT dogrulama basarisiz [{}]: {}", path, ex.getMessage());
            return unauthorized(exchange, "Gecersiz veya suresi dolmus token.");
        }

        String userId = jwtValidator.extractUserId(claims);
        String role = jwtValidator.extractRole(claims);

        // Client'in gonderdigi X-User-* header'larini TEMIZLE, gateway'inkileri koy.
        // Authorization header'ina DOKUNMA (downstream kendi filter'iyla tekrar cozer).
        ServerHttpRequest mutated = request.mutate()
                .headers(h -> {
                    h.remove(HEADER_USER_ID);
                    h.remove(HEADER_USER_ROLE);
                    if (userId != null) {
                        h.add(HEADER_USER_ID, userId);
                    }
                    if (role != null && !role.isEmpty()) {
                        h.add(HEADER_USER_ROLE, role);
                    }
                })
                .build();

        return chain.filter(exchange.mutate().request(mutated).build());
    }

    private boolean isPublic(ServerHttpRequest request) {
        String path = request.getURI().getPath();

        // CORS preflight: tarayici Authorization header'i gondermez, 401 CORS hatasina donusur.
        if (HttpMethod.OPTIONS.equals(request.getMethod())) {
            return true;
        }

        for (String exact : PUBLIC_PATH_EXACT) {
            if (path.equals(exact)) {
                return true;
            }
        }
        for (String prefix : PUBLIC_PATH_PREFIXES) {
            if (path.startsWith(prefix)) {
                return true;
            }
        }
        for (PublicEndpoint endpoint : PUBLIC_ENDPOINTS) {
            if (endpoint.matches(request.getMethod(), path)) {
                return true;
            }
        }
        return false;
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        ErrorResponse body = new ErrorResponse(
                LocalDateTime.now(),
                HttpStatus.UNAUTHORIZED.value(),
                message,
                exchange.getRequest().getURI().getPath()
        );

        byte[] bytes;
        try {
            bytes = objectMapper.writeValueAsBytes(body);
        } catch (Exception e) {
            bytes = ("{\"status\":401,\"message\":\"" + message + "\"}").getBytes(StandardCharsets.UTF_8);
        }
        DataBuffer buffer = response.bufferFactory().wrap(bytes);
        return response.writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        // CORS filtresinden sonra, routing'den once calissin.
        return -100;
    }

    /**
     * Method + Ant path pattern cifti. Ayni path'in farkli method'larda farkli yetki
     * gerektirdigi durumlar icin (bkz. PUBLIC_ENDPOINTS).
     */
    private record PublicEndpoint(HttpMethod method, String pattern) {

        boolean matches(HttpMethod requestMethod, String requestPath) {
            return this.method.equals(requestMethod) && PATH_MATCHER.match(this.pattern, requestPath);
        }
    }
}