package com.gateway.unit;

import com.gateway.config.CorsConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * CorsConfig whitebox unit testleri: preflight kabul/red ve CORS header'lari.
 */
class CorsConfigTest {

    private CorsWebFilter corsWebFilter;
    private AtomicBoolean chainCalled;
    private WebFilterChain chain;

    @BeforeEach
    void setUp() {
        corsWebFilter = new CorsConfig().corsWebFilter();
        chainCalled = new AtomicBoolean(false);
        chain = (ServerWebExchange exchange) -> {
            chainCalled.set(true);
            return Mono.empty();
        };
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "http://localhost:5173",
            "http://localhost:3001",
            "http://localhost:6005",
            "http://localhost:6008"
    })
    @DisplayName("U1: corsWebFilter - Izin verilen origin'lerden gelen preflight kabul edilir ve zincire GIRMEZ")
    void corsWebFilter_WhenPreflightFromAllowedOrigin_ShouldShortCircuitWithCorsHeaders(String origin) {
        MockServerWebExchange exchange = preflight(origin, "POST");

        corsWebFilter.filter(exchange, chain).block();

        assertThat(chainCalled).isFalse();
        assertThat(exchange.getResponse().getStatusCode()).isNotEqualTo(HttpStatus.FORBIDDEN);
        HttpHeaders headers = exchange.getResponse().getHeaders();
        assertThat(headers.getAccessControlAllowOrigin()).isEqualTo(origin);
        assertThat(headers.getAccessControlAllowCredentials()).isTrue();
        assertThat(headers.getAccessControlMaxAge()).isEqualTo(3600L);
    }

    @Test
    @DisplayName("U2: corsWebFilter - Izin verilmeyen origin'den gelen preflight 403 ile reddedilir")
    void corsWebFilter_WhenPreflightFromDisallowedOrigin_ShouldReturnForbidden() {
        MockServerWebExchange exchange = preflight("http://evil.example.com", "POST");

        corsWebFilter.filter(exchange, chain).block();

        assertThat(chainCalled).isFalse();
        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(exchange.getResponse().getHeaders().getAccessControlAllowOrigin()).isNull();
    }

    @Test
    @DisplayName("U3: corsWebFilter - Preflight'ta istenen method allowed listesindeyse izin verilir")
    void corsWebFilter_WhenRequestedMethodIsAllowed_ShouldExposeAllowedMethods() {
        MockServerWebExchange exchange = preflight("http://localhost:5173", "DELETE");

        corsWebFilter.filter(exchange, chain).block();

        assertThat(exchange.getResponse().getHeaders().getAccessControlAllowMethods())
                .contains(HttpMethod.GET, HttpMethod.POST, HttpMethod.PUT,
                        HttpMethod.PATCH, HttpMethod.DELETE, HttpMethod.OPTIONS);
    }

    @Test
    @DisplayName("U4: corsWebFilter - Izin verilmeyen method (TRACE) icin preflight 403 doner")
    void corsWebFilter_WhenRequestedMethodIsNotAllowed_ShouldReturnForbidden() {
        MockServerWebExchange exchange = preflight("http://localhost:5173", "TRACE");

        corsWebFilter.filter(exchange, chain).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("U5: corsWebFilter - Normal (preflight olmayan) istek zincire devam eder ve CORS header'i alir")
    void corsWebFilter_WhenSimpleRequestFromAllowedOrigin_ShouldContinueChain() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get(GATEWAY_BASE + "/api/v1/products/search")
                        .header(HttpHeaders.ORIGIN, "http://localhost:5173")
                        .build());

        corsWebFilter.filter(exchange, chain).block();

        assertThat(chainCalled).isTrue();
        assertThat(exchange.getResponse().getHeaders().getAccessControlAllowOrigin())
                .isEqualTo("http://localhost:5173");
    }

    @Test
    @DisplayName("U6: corsWebFilter - Origin header'i olmayan istek CORS mantigina takilmadan gecer")
    void corsWebFilter_WhenNoOriginHeader_ShouldPassThroughUntouched() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get(GATEWAY_BASE + "/api/orders/1").build());

        corsWebFilter.filter(exchange, chain).block();

        assertThat(chainCalled).isTrue();
        assertThat(exchange.getResponse().getHeaders().getAccessControlAllowOrigin()).isNull();
    }

    @Test
    @DisplayName("U7: corsWebFilter - Same-origin istek CORS islemine hic girmeden zincire devam eder")
    void corsWebFilter_WhenRequestIsSameOrigin_ShouldSkipCorsProcessing() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get(GATEWAY_BASE + "/api/orders/1")
                        .header(HttpHeaders.ORIGIN, GATEWAY_BASE)
                        .build());

        corsWebFilter.filter(exchange, chain).block();

        assertThat(chainCalled).isTrue();
    }

    /**
     * CorsUtils.isSameOrigin istegin MUTLAK URI'sine (scheme + host + port) bakar;
     * bagil path'li bir mock istek "Actual request scheme must not be null" hatasi verir.
     * Bu yuzden testlerde gateway'in gercek adresi kullanilir.
     */
    private static final String GATEWAY_BASE = "http://localhost:8080";

    private static MockServerWebExchange preflight(String origin, String requestedMethod) {
        return MockServerWebExchange.from(
                MockServerHttpRequest.options(GATEWAY_BASE + "/api/orders/1")
                        .header(HttpHeaders.ORIGIN, origin)
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, requestedMethod)
                        .build());
    }
}
