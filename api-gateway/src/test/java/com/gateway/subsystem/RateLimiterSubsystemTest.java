package com.gateway.subsystem;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.reactive.server.EntityExchangeResult;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * S: Gercek Redis uzerinde calisan RequestRateLimiter davranisi.
 * Docker yoksa bu sinif tamamen atlanir (bkz. AbstractGatewaySubsystemTest).
 */
class RateLimiterSubsystemTest extends AbstractGatewaySubsystemTest {

    private static final int BURST_ATTEMPTS = BURST_CAPACITY + 5;

    @Test
    @DisplayName("S1: Kova kapasitesi asilinca gateway 429 doner (gercek Redis sayaci)")
    void rateLimiter_WhenBurstExceedsCapacity_ShouldReturnTooManyRequests() {
        String auth = bearer(uniqueUserId("user-burst"), "ROLE_USER");

        List<Integer> statuses = fire(BURST_ATTEMPTS, auth);

        assertThat(statuses.get(0)).isEqualTo(200);
        assertThat(statuses).contains(429);
        assertThat(statuses.stream().filter(s -> s == 200).count())
                .isLessThanOrEqualTo((long) BURST_CAPACITY + 1);
    }

    @Test
    @DisplayName("S2: Limit anahtari kullanici bazlidir - bir kullanicinin limiti digerini etkilemez")
    void rateLimiter_WhenOneUserIsThrottled_ShouldNotAffectAnotherUser() {
        String throttled = bearer(uniqueUserId("user-a"), "ROLE_USER");
        fire(BURST_ATTEMPTS, throttled);

        String fresh = bearer(uniqueUserId("user-b"), "ROLE_USER");
        List<Integer> freshStatuses = fire(1, fresh);

        assertThat(freshStatuses).containsExactly(200);
    }

    @Test
    @DisplayName("S3: 429 yaniti ErrorResponse govdesiyle ve Turkce limit mesajiyla doner")
    void rateLimiter_WhenThrottled_ShouldReturnErrorResponseBody() {
        String auth = bearer(uniqueUserId("user-body"), "ROLE_USER");

        EntityExchangeResult<byte[]> throttledResult = null;
        for (int i = 0; i < BURST_ATTEMPTS && throttledResult == null; i++) {
            EntityExchangeResult<byte[]> result = webTestClient.get().uri("/api/orders/1")
                    .header(HttpHeaders.AUTHORIZATION, auth)
                    .exchange()
                    .expectBody().returnResult();
            if (result.getStatus().value() == 429) {
                throttledResult = result;
            }
        }

        assertThat(throttledResult).as("burst sonunda en az bir 429 beklenir").isNotNull();
        String body = new String(throttledResult.getResponseBodyContent() == null
                ? new byte[0] : throttledResult.getResponseBodyContent());
        assertThat(body).contains("429").contains("Istek limiti asildi");
    }

    @Test
    @DisplayName("S4: Limitli route rate limit header'larini (X-RateLimit-*) doner")
    void rateLimiter_WhenRequestIsAllowed_ShouldExposeRateLimitHeaders() {
        String auth = bearer(uniqueUserId("user-hdr"), "ROLE_USER");

        webTestClient.get().uri("/api/orders/1")
                .header(HttpHeaders.AUTHORIZATION, auth)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().exists("X-RateLimit-Remaining")
                .expectHeader().valueEquals("X-RateLimit-Burst-Capacity", String.valueOf(BURST_CAPACITY))
                .expectHeader().valueEquals("X-RateLimit-Replenish-Rate", String.valueOf(REPLENISH_RATE));
    }

    @Test
    @DisplayName("S5: Limitsiz route (auth) ayni yogunlukta istekte hic 429 dondurmez")
    void rateLimiter_WhenRouteHasNoLimiter_ShouldNeverThrottle() {
        List<Integer> statuses = new ArrayList<>();
        for (int i = 0; i < BURST_ATTEMPTS; i++) {
            statuses.add(webTestClient.post().uri("/api/v1/auth/login")
                    .bodyValue("{}")
                    .exchange()
                    .returnResult(byte[].class).getStatus().value());
        }

        assertThat(statuses).doesNotContain(429).containsOnly(200);
    }

    @Test
    @DisplayName("S6: Kimliksiz public istekler IP anahtariyla limitlenir (userKeyResolver fallback'i)")
    void rateLimiter_WhenRequestHasNoUserId_ShouldThrottleByClientIp() {
        List<Integer> statuses = new ArrayList<>();
        for (int i = 0; i < BURST_ATTEMPTS + 5; i++) {
            statuses.add(webTestClient.get().uri("/api/v1/products/search?q=x")
                    .exchange()
                    .returnResult(byte[].class).getStatus().value());
        }

        assertThat(statuses).contains(429);
    }

    private List<Integer> fire(int times, String authorization) {
        List<Integer> statuses = new ArrayList<>();
        for (int i = 0; i < times; i++) {
            statuses.add(webTestClient.get().uri("/api/orders/1")
                    .header(HttpHeaders.AUTHORIZATION, authorization)
                    .exchange()
                    .returnResult(byte[].class).getStatus().value());
        }
        return statuses;
    }
}
