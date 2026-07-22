package com.review.systemalpha;

import com.review.unit.support.JwtTestTokens;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * System-Alpha: uçtan uca, TAM STACK, BLACK-BOX.
 *
 * Sisteme yalnızca public HTTP API'den ve RabbitMQ'dan girilir; hiçbir
 * repository/adapter/use case bean'i inject edilmez. Test, review-service'i
 * dışarıdaki bir istemci (mobil uygulama / storefront / admin paneli) gibi
 * kullanır.
 *
 * Kapsanan uçtan uca akış:
 *   order.shipped event'i -> yorum oluştur -> moderasyon -> mağaza cevabı ->
 *   arama/listeleme
 *
 * NOT: Docker gerektirir; Docker'sız makinede çalıştırılamaz.
 */
@DisplayName("Review Service - System Alpha (tam stack, black-box uçtan uca)")
@AutoConfigureTestRestTemplate
class ReviewLifecycleSystemAlphaTest extends AbstractReviewSystemAlphaTest {

    private static final String REVIEW_EVENT_QUEUE = "e2e.observer.review.events.q";

    @LocalServerPort
    private int port;

    /** Sadece HTTP istemcisi - uygulamanın kendi bean'i değildir. */
    @Autowired
    private TestRestTemplate http;

    private RabbitTemplate broker;
    private RabbitAdmin brokerAdmin;

    @BeforeEach
    void connectExternalObserver() {
        broker = externalRabbitClient();
        brokerAdmin = externalRabbitAdmin();

        // Dışarıdaki bir tüketici gibi review.exchange'i dinleyen gözlemci kuyruk.
        Queue queue = QueueBuilder.durable(REVIEW_EVENT_QUEUE).build();
        TopicExchange reviewExchange = new TopicExchange(REVIEW_EXCHANGE);
        brokerAdmin.declareExchange(reviewExchange);
        brokerAdmin.declareQueue(queue);
        brokerAdmin.declareBinding(BindingBuilder.bind(queue).to(reviewExchange).with("review.#"));
        brokerAdmin.purgeQueue(REVIEW_EVENT_QUEUE, true);

        brokerAdmin.declareExchange(new TopicExchange(ORDER_EXCHANGE));
    }

    // ------------------------------------------------------------------
    // Yalnızca dış arayüzleri kullanan yardımcılar
    // ------------------------------------------------------------------

    private String url(String path) {
        return "http://localhost:" + port + path;
    }

    private HttpHeaders headers(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (token != null) {
            headers.setBearerAuth(token);
        }
        return headers;
    }

    /** Sipariş kargolandı event'ini broker'a yayınlar (dış sistem simülasyonu). */
    private void publishOrderShipped(String orderId, String customerId, String productId) {
        String payload = String.format(
                "{\"orderId\":\"%s\",\"customerId\":\"%s\",\"shippedAt\":\"2026-01-15T10:30:00\","
                        + "\"items\":[{\"productId\":\"%s\",\"quantity\":1}]}",
                orderId, customerId, productId);
        broker.convertAndSend(ORDER_EXCHANGE, ORDER_SHIPPED_ROUTING_KEY, payload);
    }

    private ResponseEntity<String> createReview(String customerToken, String orderId,
                                                String productId, int rating, String comment) {
        String body = String.format(
                "{\"orderId\":\"%s\",\"productId\":\"%s\",\"rating\":%d,\"comment\":\"%s\"}",
                orderId, productId, rating, comment);
        return http.exchange(url("/api/reviews"), HttpMethod.POST,
                new HttpEntity<>(body, headers(customerToken)), String.class);
    }

    /**
     * order.shipped event'inin işlenmesi asenkrondur. Uygunluğun oluştuğunu
     * DB'ye bakarak DEĞİL, yorum oluşturma çağrısının başarılı olmasıyla
     * anlarız - bu tamamen black-box bir gözlemdir.
     */
    private String createReviewAfterShipment(String customerId, String productId,
                                             int rating, String comment) {
        String orderId = "order-" + UUID.randomUUID();
        publishOrderShipped(orderId, customerId, productId);

        String token = JwtTestTokens.customerToken(customerId);
        AtomicBoolean created = new AtomicBoolean(false);
        StringBuilder reviewId = new StringBuilder();

        await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> {
            if (created.get()) {
                return;
            }
            ResponseEntity<String> response =
                    createReview(token, orderId, productId, rating, comment);
            assertThat(response.getStatusCode())
                    .as("uygunluk henüz oluşmadı, yanıt: %s", response.getBody())
                    .isEqualTo(HttpStatus.OK);
            created.set(true);
            reviewId.append(response.getBody());
        });

        return reviewId.toString();
    }

    private String productReviewsJson(String productId) {
        return http.getForObject(url("/api/reviews/product/" + productId), String.class);
    }

    private boolean drainQueueLookingFor(String needle) {
        AtomicBoolean found = new AtomicBoolean(false);
        Message message;
        while (!found.get() && (message = broker.receive(REVIEW_EVENT_QUEUE, 200)) != null) {
            if (new String(message.getBody(), StandardCharsets.UTF_8).contains(needle)) {
                found.set(true);
            }
        }
        return found.get();
    }

    // ------------------------------------------------------------------
    // Testler
    // ------------------------------------------------------------------

    @Test
    @DisplayName("A1: Kargo event'i -> yorum -> moderasyon -> mağaza cevabı -> listeleme (tam uçtan uca akış)")
    void fullReviewLifecycle_FromShipmentToPublicListing_ShouldWorkEndToEnd() {
        String productId = "prod-a1";
        String customerId = "cust-a1";

        // 1) Sipariş kargolanır ve müşteri yorum yazar.
        String reviewId = createReviewAfterShipment(customerId, productId, 4, "gayet iyi");
        assertThat(reviewId).isNotBlank();

        // 2) Yorum herkese açık ürün listelemesinde görünür.
        await().atMost(Duration.ofSeconds(20)).untilAsserted(() ->
                assertThat(productReviewsJson(productId))
                        .contains(reviewId)
                        .contains("gayet iyi")
                        .contains("\"averageRating\":4.0"));

        // 3) Mağaza yoruma cevap yazar.
        ResponseEntity<String> reply = http.exchange(url("/api/reviews/" + reviewId + "/reply"),
                HttpMethod.PATCH,
                new HttpEntity<>("{\"replyText\":\"Ilginiz icin tesekkurler\"}",
                        headers(JwtTestTokens.storeToken("store-a1"))),
                String.class);
        assertThat(reply.getStatusCode()).isEqualTo(HttpStatus.OK);

        // 4) Mağaza cevabı da herkese açık listelemede görünür.
        await().atMost(Duration.ofSeconds(20)).untilAsserted(() ->
                assertThat(productReviewsJson(productId)).contains("Ilginiz icin tesekkurler"));

        // 5) Admin yorumu gizler -> listeden düşer.
        ResponseEntity<String> hide = http.exchange(url("/api/reviews/" + reviewId + "/moderate"),
                HttpMethod.PATCH,
                new HttpEntity<>("{\"status\":\"HIDDEN\"}",
                        headers(JwtTestTokens.adminToken("admin-a1"))),
                String.class);
        assertThat(hide.getStatusCode()).isEqualTo(HttpStatus.OK);

        await().atMost(Duration.ofSeconds(20)).untilAsserted(() ->
                assertThat(productReviewsJson(productId)).doesNotContain(reviewId));

        // 6) Admin kararını geri alır -> yorum tekrar görünür.
        http.exchange(url("/api/reviews/" + reviewId + "/moderate"), HttpMethod.PATCH,
                new HttpEntity<>("{\"status\":\"ACTIVE\"}",
                        headers(JwtTestTokens.adminToken("admin-a1"))),
                String.class);

        await().atMost(Duration.ofSeconds(20)).untilAsserted(() ->
                assertThat(productReviewsJson(productId)).contains(reviewId));
    }

    @Test
    @DisplayName("A2: Yorum oluşturulduğunda review.created event'i broker'a düşer (dış tüketici gözlemi)")
    void createReview_ShouldEventuallyPublishReviewCreatedEventToBroker() {
        String reviewId = createReviewAfterShipment("cust-a2", "prod-a2", 5, "mukemmel");
        assertThat(reviewId).isNotBlank();

        // Outbox publisher scheduler'ı açık; event kendiliğinden yayınlanmalı.
        await().atMost(Duration.ofSeconds(30))
                .untilAsserted(() -> assertThat(drainQueueLookingFor("prod-a2")).isTrue());
    }

    @Test
    @DisplayName("A3: Satın alınmamış ürüne yorum yazılamaz ve listelemede hiç görünmez")
    void createReview_WithoutShipment_ShouldBeRejectedAndNeverAppearPublicly() {
        ResponseEntity<String> response = createReview(
                JwtTestTokens.customerToken("cust-a3"),
                "order-hic-kargolanmadi-" + UUID.randomUUID(),
                "prod-a3", 5, "sahte yorum");

        assertThat(response.getStatusCode().is2xxSuccessful()).isFalse();
        // /error permitAll bugfix'i sayesinde gerçek hata 403 maskesi altında gizlenmez.
        assertThat(response.getStatusCode()).isNotEqualTo(HttpStatus.FORBIDDEN);

        assertThat(productReviewsJson("prod-a3")).doesNotContain("sahte yorum");
    }

    @Test
    @DisplayName("A4: Aynı sipariş+ürün için ikinci yorum reddedilir, listelemede tek yorum kalır")
    void createReview_Twice_ShouldRejectDuplicateAndKeepSingleReviewPublicly() {
        String productId = "prod-a4";
        String customerId = "cust-a4";
        String orderId = "order-" + UUID.randomUUID();
        publishOrderShipped(orderId, customerId, productId);

        String token = JwtTestTokens.customerToken(customerId);

        await().atMost(Duration.ofSeconds(30)).untilAsserted(() ->
                assertThat(createReview(token, orderId, productId, 5, "ilk yorum").getStatusCode())
                        .isEqualTo(HttpStatus.OK));

        ResponseEntity<String> second = createReview(token, orderId, productId, 1, "ikinci yorum");
        assertThat(second.getStatusCode().is2xxSuccessful()).isFalse();

        await().atMost(Duration.ofSeconds(20)).untilAsserted(() ->
                assertThat(productReviewsJson(productId))
                        .contains("ilk yorum")
                        .doesNotContain("ikinci yorum"));
    }

    @Test
    @DisplayName("A5: Yetkilendirme matrisi - Her endpoint yalnızca kendi rolünü kabul eder (gerçek HTTP)")
    void endpointAuthorization_ShouldRejectWrongRolesOverRealHttp() {
        String customerToken = JwtTestTokens.customerToken("cust-a5");
        String storeToken = JwtTestTokens.storeToken("store-a5");
        String adminToken = JwtTestTokens.adminToken("admin-a5");

        // Moderasyon: yalnızca ADMIN
        assertThat(patchStatus("/api/reviews/x/moderate", "{\"status\":\"HIDDEN\"}", customerToken))
                .isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(patchStatus("/api/reviews/x/moderate", "{\"status\":\"HIDDEN\"}", storeToken))
                .isEqualTo(HttpStatus.FORBIDDEN);

        // Mağaza cevabı: yalnızca STORE
        assertThat(patchStatus("/api/reviews/x/reply", "{\"replyText\":\"a\"}", customerToken))
                .isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(patchStatus("/api/reviews/x/reply", "{\"replyText\":\"a\"}", adminToken))
                .isEqualTo(HttpStatus.FORBIDDEN);

        // Moderasyon listesi: yalnızca ADMIN
        assertThat(http.exchange(url("/api/reviews/all"), HttpMethod.GET,
                new HttpEntity<>(headers(customerToken)), String.class).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(http.exchange(url("/api/reviews/all"), HttpMethod.GET,
                new HttpEntity<>(headers(adminToken)), String.class).getStatusCode())
                .isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("A6: Kimliksiz istekler - Korumalı uçlar reddedilir, ürün yorumları herkese açıktır")
    void anonymousAccess_ShouldBeAllowedOnlyOnPublicProductEndpoint() {
        // NOT: SecurityConfig'te özel bir AuthenticationEntryPoint tanımlı
        // olmadığı için Spring Security varsayılan Http403ForbiddenEntryPoint'i
        // kullanır; kimliksiz istek 401 değil 403 alır. Test gerçek davranışı
        // sabitler (401'e geçiş ayrı bir ürün kararıdır).
        assertThat(http.exchange(url("/api/reviews/me"), HttpMethod.GET,
                new HttpEntity<>(headers(null)), String.class).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);

        assertThat(http.exchange(url("/api/reviews/all"), HttpMethod.GET,
                new HttpEntity<>(headers(null)), String.class).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);

        assertThat(createReview(null, "order-1", "prod-a6", 5, "anonim").getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);

        // Ürün yorumları herkese açık
        assertThat(http.getForEntity(url("/api/reviews/product/prod-a6"), String.class)
                .getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("A7: Geçersiz imzalı token gerçek uçta reddedilir")
    void tamperedToken_ShouldBeRejectedByRunningService() {
        String forged = JwtTestTokens.tokenSignedWithWrongSecret("saldirgan", "ADMIN");

        assertThat(http.exchange(url("/api/reviews/all"), HttpMethod.GET,
                new HttpEntity<>(headers(forged)), String.class).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("A8: Ortalama puan - Birden fazla yorumdan hesaplanır ve moderasyonla güncellenir")
    void productAverageRating_ShouldReflectOnlyVisibleReviews() {
        String productId = "prod-a8";

        createReviewAfterShipment("cust-a8-1", productId, 5, "harika");
        String lowRatedId = createReviewAfterShipment("cust-a8-2", productId, 1, "berbat");

        await().atMost(Duration.ofSeconds(20)).untilAsserted(() ->
                assertThat(productReviewsJson(productId)).contains("\"averageRating\":3.0"));

        http.exchange(url("/api/reviews/" + lowRatedId + "/moderate"), HttpMethod.PATCH,
                new HttpEntity<>("{\"status\":\"HIDDEN\"}",
                        headers(JwtTestTokens.adminToken("admin-a8"))),
                String.class);

        await().atMost(Duration.ofSeconds(20)).untilAsserted(() ->
                assertThat(productReviewsJson(productId)).contains("\"averageRating\":5.0"));
    }

    @Test
    @DisplayName("A9: Müşteri kendi yorumlarını /me ucundan görebilir, başkasınınkini göremez")
    void getMyReviews_ShouldReturnOnlyCallersOwnReviewsOverHttp() {
        String reviewId = createReviewAfterShipment("cust-a9", "prod-a9", 4, "iyi urun");

        await().atMost(Duration.ofSeconds(20)).untilAsserted(() -> {
            ResponseEntity<String> mine = http.exchange(url("/api/reviews/me"), HttpMethod.GET,
                    new HttpEntity<>(headers(JwtTestTokens.customerToken("cust-a9"))), String.class);
            assertThat(mine.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(mine.getBody()).contains(reviewId);
        });

        ResponseEntity<String> others = http.exchange(url("/api/reviews/me"), HttpMethod.GET,
                new HttpEntity<>(headers(JwtTestTokens.customerToken("baskasi-a9"))), String.class);
        assertThat(others.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(others.getBody()).doesNotContain(reviewId);
    }

    @Test
    @DisplayName("A10: Geçersiz moderasyon durumu 400 döner (500 değil) [BUGFIX regresyonu]")
    void moderateReview_WithInvalidOrMissingStatus_ShouldReturn400OverHttp() {
        String adminToken = JwtTestTokens.adminToken("admin-a10");

        assertThat(patchStatus("/api/reviews/x/moderate", "{\"status\":\"DELETED\"}", adminToken))
                .isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(patchStatus("/api/reviews/x/moderate", "{\"status\":null}", adminToken))
                .isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(patchStatus("/api/reviews/x/moderate", "{}", adminToken))
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("A11: Geçersiz puan değeri gerçek uçta 400 döner")
    void createReview_WithOutOfRangeRating_ShouldReturn400OverHttp() {
        String token = JwtTestTokens.customerToken("cust-a11");

        ResponseEntity<String> response = createReview(token, "order-a11", "prod-a11", 9, "abartili");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    private HttpStatus patchStatus(String path, String body, String token) {
        return (HttpStatus) http.exchange(url(path), HttpMethod.PATCH,
                new HttpEntity<>(body, headers(token)), String.class).getStatusCode();
    }
}
