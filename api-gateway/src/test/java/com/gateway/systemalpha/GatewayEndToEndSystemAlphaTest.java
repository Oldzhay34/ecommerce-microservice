package com.gateway.systemalpha;

import com.gateway.support.JwtTestTokens;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A: TAM STACK uctan uca BLACK-BOX testi.
 *
 * Kurallar:
 *  - Teste yalnizca gateway'in public HTTP portundan girilir (java.net.http.HttpClient).
 *  - Hicbir Spring bean'i inject EDILMEZ (WebTestClient, JwtValidator, KeyResolver yok).
 *    Sadece dinlenen port ogrenilir.
 *  - Redis gercek container'dir, downstream servis gercek bir HTTP sunucusudur.
 *
 * Senaryo: token'siz istek reddedilir -> gecerli token'la istek downstream'e proxy'lenir
 * -> ayni kullanici limiti asinca 429 alir.
 *
 * Docker yoksa sinif komple atlanir.
 */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class GatewayEndToEndSystemAlphaTest {

    private static final int REPLENISH_RATE = 1;
    private static final int BURST_CAPACITY = 3;

    @Container
    private static final GenericContainer<?> REDIS =
            new GenericContainer<>(DockerImageName.parse("redis:7-alpine")).withExposedPorts(6379);

    /** Gercek bir siparis servisi gibi davranan sahte HTTP sunucusu. */
    private static final MockWebServer ORDER_SERVICE = new MockWebServer();

    /** Sistem disindan bakan tek istemci. */
    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    /** Senaryo boyunca kullanilan sabit kullanici; rate limit kovasi bu kimlige baglidir. */
    private static final String SCENARIO_USER = "e2e-user-1";

    @LocalServerPort
    private int gatewayPort;

    static {
        try {
            ORDER_SERVICE.start();
        } catch (IOException e) {
            throw new IllegalStateException("Sahte order-service baslatilamadi", e);
        }
        ORDER_SERVICE.setDispatcher(new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) {
                return new MockResponse()
                        .setResponseCode(200)
                        .setHeader("Content-Type", "application/json")
                        .setBody("{\"id\":\"1\",\"owner\":\"" + request.getHeader("X-User-Id") + "\"}");
            }
        });
    }

    @DynamicPropertySource
    static void systemProperties(DynamicPropertyRegistry registry) {
        String orderService = "http://" + ORDER_SERVICE.getHostName() + ":" + ORDER_SERVICE.getPort();

        registry.add("app.security.jwt-secret", () -> JwtTestTokens.SECRET);
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
        registry.add("spring.data.redis.database", () -> "0");

        String prefix = "spring.cloud.gateway.routes[0]";
        registry.add(prefix + ".id", () -> "e2e-order-service");
        registry.add(prefix + ".uri", () -> orderService);
        registry.add(prefix + ".predicates[0]", () -> "Path=/api/orders/**");
        registry.add(prefix + ".filters[0].name", () -> "RequestRateLimiter");
        registry.add(prefix + ".filters[0].args.redis-rate-limiter.replenishRate", () -> String.valueOf(REPLENISH_RATE));
        registry.add(prefix + ".filters[0].args.redis-rate-limiter.burstCapacity", () -> String.valueOf(BURST_CAPACITY));
        registry.add(prefix + ".filters[0].args.key-resolver", () -> "#{@userKeyResolver}");

        registry.add("spring.cloud.gateway.routes[1].id", () -> "e2e-auth-service");
        registry.add("spring.cloud.gateway.routes[1].uri", () -> orderService);
        registry.add("spring.cloud.gateway.routes[1].predicates[0]", () -> "Path=/api/v1/auth/**");
    }

    @Test
    @Order(1)
    @DisplayName("A1: Token'siz istek gateway sinirinda reddedilir ve downstream'e hic ulasmaz")
    void scenario_WhenRequestHasNoToken_ShouldBeRejectedAtTheEdge() throws Exception {
        HttpResponse<String> response = send(get("/api/orders/1"));

        assertThat(response.statusCode()).isEqualTo(401);
        assertThat(response.body()).contains("\"status\":401");
        assertThat(ORDER_SERVICE.takeRequest(300, TimeUnit.MILLISECONDS))
                .as("reddedilen istek downstream'e sizmamalidir")
                .isNull();
    }

    @Test
    @Order(2)
    @DisplayName("A2: Suresi dolmus token da ayni sekilde 401 ile reddedilir")
    void scenario_WhenTokenIsExpired_ShouldBeRejected() throws Exception {
        HttpResponse<String> response = send(get("/api/orders/1")
                .header("Authorization", "Bearer " + JwtTestTokens.expired(SCENARIO_USER)));

        assertThat(response.statusCode()).isEqualTo(401);
        assertThat(ORDER_SERVICE.takeRequest(300, TimeUnit.MILLISECONDS)).isNull();
    }

    @Test
    @Order(3)
    @DisplayName("A3: Gecerli token ile istek downstream'e proxy'lenir ve kimlik header'i uctan uca tasinir")
    void scenario_WhenTokenIsValid_ShouldReachDownstreamWithResolvedIdentity() throws Exception {
        HttpResponse<String> response = send(get("/api/orders/1")
                .header("Authorization", "Bearer " + JwtTestTokens.valid(SCENARIO_USER, "ROLE_USER")));

        assertThat(response.statusCode()).isEqualTo(200);
        // Sahte servis, gateway'in cozdugu X-User-Id'yi govdeye yansitiyor.
        assertThat(response.body()).contains("\"owner\":\"" + SCENARIO_USER + "\"");

        RecordedRequest recorded = ORDER_SERVICE.takeRequest(5, TimeUnit.SECONDS);
        assertThat(recorded).isNotNull();
        assertThat(recorded.getPath()).isEqualTo("/api/orders/1");
        assertThat(recorded.getHeader("X-User-Id")).isEqualTo(SCENARIO_USER);
        assertThat(recorded.getHeader("X-User-Role")).isEqualTo("ROLE_USER");
    }

    @Test
    @Order(4)
    @DisplayName("A4: Ayni kullanici limiti asinca gateway 429 doner (gercek Redis sayaci)")
    void scenario_WhenSameUserExceedsRateLimit_ShouldReceiveTooManyRequests() throws Exception {
        String authorization = "Bearer " + JwtTestTokens.valid(SCENARIO_USER, "ROLE_USER");
        List<Integer> statuses = new ArrayList<>();

        for (int i = 0; i < BURST_CAPACITY + 5; i++) {
            statuses.add(send(get("/api/orders/1").header("Authorization", authorization)).statusCode());
        }

        assertThat(statuses).contains(429);
        assertThat(statuses).doesNotContain(401, 500);
    }

    @Test
    @Order(5)
    @DisplayName("A5: Public auth route'u token'siz calisir ve rate limit'e takilmaz")
    void scenario_WhenPublicAuthRouteCalled_ShouldWorkWithoutTokenAndWithoutThrottling() throws Exception {
        List<Integer> statuses = new ArrayList<>();
        for (int i = 0; i < BURST_CAPACITY + 5; i++) {
            statuses.add(send(HttpRequest.newBuilder(uri("/api/v1/auth/login"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString("{\"email\":\"a@b.c\"}")))
                    .statusCode());
        }

        assertThat(statuses).containsOnly(200);
    }

    // --- black-box istemci yardimcilari -------------------------------------

    private URI uri(String path) {
        return URI.create("http://localhost:" + gatewayPort + path);
    }

    private HttpRequest.Builder get(String path) {
        return HttpRequest.newBuilder(uri(path)).GET();
    }

    private static HttpResponse<String> send(HttpRequest.Builder builder) throws Exception {
        return CLIENT.send(builder.timeout(Duration.ofSeconds(15)).build(),
                HttpResponse.BodyHandlers.ofString());
    }
}
