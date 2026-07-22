package com.gateway.unit;

import com.gateway.config.RateLimiterConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.test.StepVerifier;

import java.net.InetSocketAddress;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Rate limiter key resolver whitebox unit testleri (Redis YOK).
 */
class RateLimiterConfigTest {

    private KeyResolver keyResolver;

    @BeforeEach
    void setUp() {
        keyResolver = new RateLimiterConfig().userKeyResolver();
    }

    @Test
    @DisplayName("U1: userKeyResolver - X-User-Id varsa limit anahtari kullanici kimligidir")
    void userKeyResolver_WhenUserIdHeaderPresent_ShouldResolveToUserId() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/orders")
                        .header("X-User-Id", "user-123")
                        .remoteAddress(new InetSocketAddress("10.0.0.5", 51000))
                        .build());

        StepVerifier.create(keyResolver.resolve(exchange))
                .expectNext("user-123")
                .verifyComplete();
    }

    @Test
    @DisplayName("U2: userKeyResolver - X-User-Id yoksa client IP adresine duser")
    void userKeyResolver_WhenUserIdHeaderMissing_ShouldFallBackToRemoteIp() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/products/search")
                        .remoteAddress(new InetSocketAddress("10.0.0.5", 51000))
                        .build());

        StepVerifier.create(keyResolver.resolve(exchange))
                .expectNext("10.0.0.5")
                .verifyComplete();
    }

    @Test
    @DisplayName("U3: userKeyResolver - X-User-Id bosluklardan ibaretse IP'ye duser (blank kontrolu)")
    void userKeyResolver_WhenUserIdHeaderIsBlank_ShouldFallBackToRemoteIp() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/orders")
                        .header("X-User-Id", "   ")
                        .remoteAddress(new InetSocketAddress("192.168.1.7", 4000))
                        .build());

        StepVerifier.create(keyResolver.resolve(exchange))
                .expectNext("192.168.1.7")
                .verifyComplete();
    }

    @Test
    @DisplayName("U4: userKeyResolver - Ne kullanici ne IP bilinemiyorsa 'unknown' doner (NPE atmaz)")
    void userKeyResolver_WhenRemoteAddressIsNull_ShouldResolveToUnknown() {
        MockServerWebExchange exchange =
                MockServerWebExchange.from(MockServerHttpRequest.get("/api/orders"));

        StepVerifier.create(keyResolver.resolve(exchange))
                .expectNext("unknown")
                .verifyComplete();
    }

    @Test
    @DisplayName("U5: userKeyResolver - Ayni kullanicinin farkli IP'lerden istegi ayni anahtara duser")
    void userKeyResolver_WhenSameUserFromDifferentIps_ShouldProduceSameKey() {
        String first = keyResolver.resolve(MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/orders")
                        .header("X-User-Id", "user-1")
                        .remoteAddress(new InetSocketAddress("10.0.0.1", 1))
                        .build())).block();

        String second = keyResolver.resolve(MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/orders")
                        .header("X-User-Id", "user-1")
                        .remoteAddress(new InetSocketAddress("10.0.0.2", 2))
                        .build())).block();

        assertThat(first).isEqualTo(second).isEqualTo("user-1");
    }
}
