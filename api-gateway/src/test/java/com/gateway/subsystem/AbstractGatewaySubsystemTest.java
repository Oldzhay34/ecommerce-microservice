package com.gateway.subsystem;

import com.gateway.support.JwtTestTokens;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.io.IOException;
import java.time.Duration;

/**
 * Subsystem katmani: gateway + GERCEK Redis (Testcontainers) + sahte downstream.
 *
 * Integration katmanindan farki: RequestRateLimiter burada gercekten calisir; token
 * bucket sayaclari Redis'te tutulur. Bu sayede "limit asilinca 429" gibi davranislar
 * mock'lanmadan, gercek altyapiyla dogrulanir.
 *
 * disabledWithoutDocker = true: Docker bulunmayan makinelerde bu sinif komple
 * ATLANIR (skip), basarisiz olmaz.
 */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
abstract class AbstractGatewaySubsystemTest {

    /** Rate limiter sayaclarinin tutuldugu gercek Redis. */
    @Container
    protected static final GenericContainer<?> REDIS =
            new GenericContainer<>(DockerImageName.parse("redis:7-alpine")).withExposedPorts(6379);

    /** Tum downstream servisleri temsil eden sahte sunucu (container gerektirmez). */
    protected static final MockWebServer DOWNSTREAM = new MockWebServer();

    /** Testlerde kullanilan limit: saniyede 1 token, kova kapasitesi 3. */
    protected static final int REPLENISH_RATE = 1;
    protected static final int BURST_CAPACITY = 3;

    static {
        try {
            DOWNSTREAM.start();
        } catch (IOException e) {
            throw new IllegalStateException("MockWebServer baslatilamadi", e);
        }
        // Rate limit testleri cok sayida istek atar; kuyruk yerine her istege 200 donen
        // bir dispatcher kullanilir.
        DOWNSTREAM.setDispatcher(new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) {
                return new MockResponse().setResponseCode(200)
                        .setHeader("Content-Type", "application/json")
                        .setBody("{\"ok\":true}");
            }
        });
    }

    @Autowired
    protected WebTestClient webTestClient;

    @DynamicPropertySource
    static void subsystemProperties(DynamicPropertyRegistry registry) {
        String downstream = "http://" + DOWNSTREAM.getHostName() + ":" + DOWNSTREAM.getPort();

        registry.add("app.security.jwt-secret", () -> JwtTestTokens.SECRET);
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
        // Her test sinifi kendi Redis veritabanini kullansin diye sabit bir db secilir.
        registry.add("spring.data.redis.database", () -> "0");

        // [0] limitli korumali route
        rateLimitedRoute(registry, 0, "sub-orders", downstream, "/api/orders/**");
        // [1] limitli public route (X-User-Id yok -> IP anahtari)
        rateLimitedRoute(registry, 1, "sub-product-search", downstream, "/api/v1/products/search");
        // [2] limitsiz public route: karsilastirma icin
        registry.add("spring.cloud.gateway.routes[2].id", () -> "sub-auth");
        registry.add("spring.cloud.gateway.routes[2].uri", () -> downstream);
        registry.add("spring.cloud.gateway.routes[2].predicates[0]", () -> "Path=/api/v1/auth/**");
    }

    private static void rateLimitedRoute(DynamicPropertyRegistry registry, int index,
                                         String id, String uri, String path) {
        String prefix = "spring.cloud.gateway.routes[" + index + "]";
        registry.add(prefix + ".id", () -> id);
        registry.add(prefix + ".uri", () -> uri);
        registry.add(prefix + ".predicates[0]", () -> "Path=" + path);
        registry.add(prefix + ".filters[0].name", () -> "RequestRateLimiter");
        registry.add(prefix + ".filters[0].args.redis-rate-limiter.replenishRate", () -> String.valueOf(REPLENISH_RATE));
        registry.add(prefix + ".filters[0].args.redis-rate-limiter.burstCapacity", () -> String.valueOf(BURST_CAPACITY));
        registry.add(prefix + ".filters[0].args.key-resolver", () -> "#{@userKeyResolver}");
    }

    @BeforeEach
    void configureClient() {
        webTestClient = webTestClient.mutate().responseTimeout(Duration.ofSeconds(20)).build();
    }

    protected static String bearer(String userId, String role) {
        return "Bearer " + JwtTestTokens.valid(userId, role);
    }

    /** Her testin kendi limit kovasini kullanmasi icin benzersiz kullanici uretir. */
    protected static String uniqueUserId(String prefix) {
        return prefix + "-" + System.nanoTime();
    }
}
