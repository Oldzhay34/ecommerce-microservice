package com.gateway.integration;

import com.gateway.support.JwtTestTokens;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Integration katmani ortak altyapisi.
 *
 * - Gateway gercek bir portta ayaga kalkar (RANDOM_PORT) ve WebTestClient ile HTTP uzerinden konusulur.
 * - Butun downstream servisler tek bir in-process MockWebServer ile taklit edilir (container YOK).
 * - application.yml'deki route listesi @DynamicPropertySource ile TAMAMEN degistirilir; boylece
 *   route'lar docker hostname'leri yerine MockWebServer adresini gosterir.
 * - Redis tabanli RequestRateLimiter bu katmanda devre disidir (gercek Redis subsystem katmaninda test edilir).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
abstract class AbstractGatewayIntegrationTest {

    /** Tum downstream servisleri temsil eden sahte sunucu. */
    protected static final MockWebServer DOWNSTREAM = new MockWebServer();

    /** Hicbir sey dinlemeyen adres: 503 senaryosu icin. */
    private static final String UNREACHABLE_URI = "http://localhost:1";

    static {
        try {
            DOWNSTREAM.start();
        } catch (IOException e) {
            throw new IllegalStateException("MockWebServer baslatilamadi", e);
        }
    }

    @Autowired
    protected WebTestClient webTestClient;

    @DynamicPropertySource
    static void gatewayProperties(DynamicPropertyRegistry registry) {
        String downstream = "http://" + DOWNSTREAM.getHostName() + ":" + DOWNSTREAM.getPort();

        registry.add("app.security.jwt-secret", () -> JwtTestTokens.SECRET);
        // Redis'e ihtiyac duyan rate limiter altyapisi bu katmanda kapali.
        registry.add("spring.cloud.gateway.redis.enabled", () -> "false");

        route(registry, 0, "test-auth", downstream, "/api/v1/auth/**");
        route(registry, 1, "test-product-search", downstream, "/api/v1/products/search");
        route(registry, 2, "test-products", downstream, "/api/v1/products/**");
        route(registry, 3, "test-orders", downstream, "/api/orders/**");
        route(registry, 4, "test-media", downstream, "/api/v1/media/**");
        route(registry, 5, "test-reviews", downstream, "/api/reviews/**");
        route(registry, 6, "test-unreachable", UNREACHABLE_URI, "/api/dead/**");
    }

    private static void route(DynamicPropertyRegistry registry, int index, String id, String uri, String path) {
        registry.add("spring.cloud.gateway.routes[" + index + "].id", () -> id);
        registry.add("spring.cloud.gateway.routes[" + index + "].uri", () -> uri);
        registry.add("spring.cloud.gateway.routes[" + index + "].predicates[0]", () -> "Path=" + path);
    }

    @BeforeEach
    void drainPendingRequests() throws InterruptedException {
        // Onceki testten artakalan kayit varsa temizle: takeRequest cagrilari tek testin
        // kendi istegini gormeli.
        while (DOWNSTREAM.takeRequest(1, TimeUnit.MILLISECONDS) != null) {
            // bosalt
        }
        webTestClient = webTestClient.mutate().responseTimeout(Duration.ofSeconds(20)).build();
    }

    // NOT: DOWNSTREAM kasitli olarak JVM boyunca acik birakilir; tum integration test
    // siniflari ayni Spring context'ini ve ayni sahte downstream'i paylasir.

    protected static String bearer(String userId, String role) {
        return "Bearer " + JwtTestTokens.valid(userId, role);
    }
}
