package com.review.subsystem;

import com.review.domain.model.ReviewStatus;
import com.review.infrastructure.messaging.publisher.OutboxEventPublisher;
import com.review.infrastructure.persistence.entity.OutboxEventJpaEntity;
import com.review.infrastructure.persistence.entity.PurchaseEligibilityJpaEntity;
import com.review.infrastructure.persistence.entity.ReviewJpaEntity;
import com.review.infrastructure.persistence.repository.OutboxEventRepository;
import com.review.infrastructure.persistence.repository.PurchaseEligibilityRepository;
import com.review.infrastructure.persistence.repository.ReviewRepository;
import com.review.infrastructure.search.document.ReviewDocument;
import com.review.infrastructure.search.repository.ReviewSearchRepository;
import com.review.unit.support.JwtTestTokens;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Subsystem katmanı: TÜM gerçek altyapı (Postgres + RabbitMQ + Elasticsearch)
 * container'da ayakta. Sisteme HTTP'den girilir, ancak subsystem seviyesinde
 * DOĞRULAMA için repository/publisher bean'lerini inject etmek serbesttir -
 * amaç iç durumun (DB satırı, ES dokümanı, outbox kaydı) doğruluğunu görmektir.
 *
 * Black-box uçtan uca akış için systemalpha katmanına bakınız.
 *
 * NOT: Docker gerektirir; Docker'sız makinede çalıştırılamaz.
 */
@DisplayName("Review Service - Subsystem (Postgres + RabbitMQ + Elasticsearch, gerçek altyapı)")
@AutoConfigureTestRestTemplate
class ReviewLifecycleSubsystemTest extends AbstractReviewSubsystemTest {

    private static final String OUTBOX_VERIFICATION_QUEUE = "test.verification.review.created.q";

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private PurchaseEligibilityRepository eligibilityRepository;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Autowired
    private ReviewSearchRepository searchRepository;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private AmqpAdmin amqpAdmin;

    /** Scheduler test'te kapalı; publisher elle tetiklenir (deterministik). */
    @Autowired
    private OutboxEventPublisher outboxEventPublisher;

    @BeforeEach
    void bindVerificationQueue() {
        Queue queue = QueueBuilder.durable(OUTBOX_VERIFICATION_QUEUE).build();
        TopicExchange exchange = new TopicExchange("review.exchange");
        Binding binding = BindingBuilder.bind(queue).to(exchange).with("review.created");

        amqpAdmin.declareQueue(queue);
        amqpAdmin.declareExchange(exchange);
        amqpAdmin.declareBinding(binding);
        amqpAdmin.purgeQueue(OUTBOX_VERIFICATION_QUEUE, true);
    }

    private String url(String path) {
        return "http://localhost:" + port + path;
    }

    private HttpHeaders authHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);
        return headers;
    }

    /** Satın alma uygunluğunu doğrudan DB'ye kurar (subsystem seviyesinde serbest). */
    private String seedEligibility(String orderId, String customerId, String productId) {
        PurchaseEligibilityJpaEntity entity = new PurchaseEligibilityJpaEntity();
        entity.setOrderId(orderId);
        entity.setCustomerId(customerId);
        entity.setProductId(productId);
        entity.setStatus("PENDING_REVIEW");
        return eligibilityRepository.save(entity).getId();
    }

    private ResponseEntity<String> postReview(String customerId, String orderId,
                                              String productId, int rating, String comment) {
        String body = String.format(
                "{\"orderId\":\"%s\",\"productId\":\"%s\",\"rating\":%d,\"comment\":\"%s\"}",
                orderId, productId, rating, comment);
        return restTemplate.exchange(url("/api/reviews"), org.springframework.http.HttpMethod.POST,
                new HttpEntity<>(body, authHeaders(JwtTestTokens.customerToken(customerId))), String.class);
    }

    @Test
    @DisplayName("S1: POST /api/reviews - Yorum Postgres'e ACTIVE olarak yazılır")
    void createReview_ShouldPersistReviewToPostgresAsActive() {
        String orderId = "order-" + UUID.randomUUID();
        String customerId = "cust-s1";
        seedEligibility(orderId, customerId, "prod-s1");

        ResponseEntity<String> response = postReview(customerId, orderId, "prod-s1", 5, "cok iyi");

        assertThat(response.getStatusCode())
                .as("beklenmeyen yanıt gövdesi: %s", response.getBody())
                .isEqualTo(HttpStatus.OK);

        String reviewId = response.getBody();
        assertThat(reviewId).isNotBlank();

        Optional<ReviewJpaEntity> persisted = reviewRepository.findById(reviewId);
        assertThat(persisted).isPresent();
        assertThat(persisted.get().getStatus()).isEqualTo(ReviewStatus.ACTIVE);
        assertThat(persisted.get().getCustomerId()).isEqualTo(customerId);
        assertThat(persisted.get().getRating()).isEqualTo(5);
        assertThat(persisted.get().getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("S2: POST /api/reviews - Yorum Elasticsearch read model'ine senkron yazılır")
    void createReview_ShouldSyncReviewToElasticsearchReadModel() {
        String orderId = "order-" + UUID.randomUUID();
        String customerId = "cust-s2";
        seedEligibility(orderId, customerId, "prod-s2");

        String reviewId = postReview(customerId, orderId, "prod-s2", 4, "guzel").getBody();

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            Optional<ReviewDocument> doc = searchRepository.findById(reviewId);
            assertThat(doc).isPresent();
            assertThat(doc.get().getStatus()).isEqualTo("ACTIVE");
            assertThat(doc.get().getProductId()).isEqualTo("prod-s2");
            assertThat(doc.get().getRating()).isEqualTo(4);
        });
    }

    @Test
    @DisplayName("S3: POST /api/reviews - Uygunluk kaydı REVIEWED'a çekilir ve ikinci yorum reddedilir")
    void createReview_ShouldMarkEligibilityReviewedAndRejectSecondAttempt() {
        String orderId = "order-" + UUID.randomUUID();
        String customerId = "cust-s3";
        seedEligibility(orderId, customerId, "prod-s3");

        assertThat(postReview(customerId, orderId, "prod-s3", 5, "ilk").getStatusCode())
                .isEqualTo(HttpStatus.OK);

        Optional<PurchaseEligibilityJpaEntity> eligibility = eligibilityRepository
                .findByOrderIdAndCustomerIdAndProductId(orderId, customerId, "prod-s3");
        assertThat(eligibility).isPresent();
        assertThat(eligibility.get().getStatus()).isEqualTo("REVIEWED");

        // İkinci deneme: uygunluk artık PENDING_REVIEW olmadığı için adapter
        // boş Optional döner -> ReviewNotEligibleException -> 5xx.
        ResponseEntity<String> second = postReview(customerId, orderId, "prod-s3", 1, "ikinci");
        assertThat(second.getStatusCode().is2xxSuccessful()).isFalse();

        assertThat(reviewRepository.findByCustomerId(customerId)).hasSize(1);
    }

    @Test
    @DisplayName("S4: POST /api/reviews - Satın alma uygunluğu yoksa yorum oluşturulamaz (hiç satır yazılmaz)")
    void createReview_WhenNotEligible_ShouldNotPersistAnything() {
        String customerId = "cust-s4";

        ResponseEntity<String> response =
                postReview(customerId, "order-never-shipped", "prod-s4", 5, "olmamali");

        assertThat(response.getStatusCode().is2xxSuccessful()).isFalse();
        assertThat(reviewRepository.findByCustomerId(customerId)).isEmpty();
    }

    @Test
    @DisplayName("S5: POST /api/reviews - Outbox tablosuna review.created kaydı yazılır")
    void createReview_ShouldWriteReviewCreatedOutboxRow() {
        String orderId = "order-" + UUID.randomUUID();
        String customerId = "cust-s5";
        seedEligibility(orderId, customerId, "prod-s5");

        String reviewId = postReview(customerId, orderId, "prod-s5", 3, "idare eder").getBody();

        List<OutboxEventJpaEntity> events = outboxEventRepository.findAll();
        assertThat(events)
                .anyMatch(e -> "review.created".equals(e.getType())
                        && reviewId.equals(e.getAggregateId())
                        && "Review".equals(e.getAggregateType())
                        && e.getPayload().contains("prod-s5"));
    }

    @Test
    @DisplayName("S6: Outbox publisher - Kaydı review.exchange/review.created ile RabbitMQ'ya yayınlar")
    void outboxPublisher_ShouldPublishReviewCreatedToRabbitMq() {
        String orderId = "order-" + UUID.randomUUID();
        String customerId = "cust-s6";
        seedEligibility(orderId, customerId, "prod-s6");

        postReview(customerId, orderId, "prod-s6", 5, "harika");

        outboxEventPublisher.publishEvents();

        AtomicBoolean found = new AtomicBoolean(false);
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            Message message;
            while (!found.get()
                    && (message = rabbitTemplate.receive(OUTBOX_VERIFICATION_QUEUE, 200)) != null) {
                if (new String(message.getBody(), StandardCharsets.UTF_8).contains("prod-s6")) {
                    found.set(true);
                }
            }
            assertThat(found).isTrue();
        });
    }

    @Test
    @DisplayName("S7: Outbox publisher - Yayınlanan kayıtlar outbox tablosundan silinir")
    void outboxPublisher_ShouldDeletePublishedRowsFromOutbox() {
        String orderId = "order-" + UUID.randomUUID();
        String customerId = "cust-s7";
        seedEligibility(orderId, customerId, "prod-s7");

        postReview(customerId, orderId, "prod-s7", 5, "temiz");
        assertThat(outboxEventRepository.findAll()).isNotEmpty();

        outboxEventPublisher.publishEvents();

        await().atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> assertThat(outboxEventRepository.findAll()).isEmpty());
    }

    @Test
    @DisplayName("S8: order.shipped event'i - RabbitMQ'dan tüketilip satın alma uygunluğu yaratır")
    void orderShippedEvent_ShouldCreatePurchaseEligibilityRow() {
        String orderId = "order-" + UUID.randomUUID();
        String payload = String.format(
                "{\"orderId\":\"%s\",\"customerId\":\"cust-s8\",\"items\":[{\"productId\":\"prod-s8\"}]}",
                orderId);

        rabbitTemplate.convertAndSend("order.exchange", "order.shipped", payload);

        await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> {
            Optional<PurchaseEligibilityJpaEntity> eligibility = eligibilityRepository
                    .findByOrderIdAndCustomerIdAndProductId(orderId, "cust-s8", "prod-s8");
            assertThat(eligibility).isPresent();
            assertThat(eligibility.get().getStatus()).isEqualTo("PENDING_REVIEW");
        });
    }

    @Test
    @DisplayName("S9: order.shipped event'i - Aynı event iki kez gelirse tek uygunluk kaydı kalır (idempotent)")
    void orderShippedEvent_WhenDeliveredTwice_ShouldRemainIdempotent() {
        String orderId = "order-" + UUID.randomUUID();
        String payload = String.format(
                "{\"orderId\":\"%s\",\"customerId\":\"cust-s9\",\"items\":[{\"productId\":\"prod-s9\"}]}",
                orderId);

        rabbitTemplate.convertAndSend("order.exchange", "order.shipped", payload);
        rabbitTemplate.convertAndSend("order.exchange", "order.shipped", payload);

        await().atMost(Duration.ofSeconds(15)).untilAsserted(() ->
                assertThat(eligibilityRepository
                        .findByOrderIdAndCustomerIdAndProductId(orderId, "cust-s9", "prod-s9"))
                        .isPresent());

        long count = eligibilityRepository.findAll().stream()
                .filter(e -> orderId.equals(e.getOrderId()) && "prod-s9".equals(e.getProductId()))
                .count();
        assertThat(count).isEqualTo(1L);
    }

    @Test
    @DisplayName("S10: PATCH /{id}/moderate - HIDDEN durumu hem Postgres'e hem Elasticsearch'e yansır")
    void moderateReview_ShouldPropagateHiddenStatusToBothStores() {
        String orderId = "order-" + UUID.randomUUID();
        String customerId = "cust-s10";
        seedEligibility(orderId, customerId, "prod-s10");
        String reviewId = postReview(customerId, orderId, "prod-s10", 1, "kotu").getBody();

        await().atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> assertThat(searchRepository.findById(reviewId)).isPresent());

        ResponseEntity<Void> response = restTemplate.exchange(
                url("/api/reviews/" + reviewId + "/moderate"),
                org.springframework.http.HttpMethod.PATCH,
                new HttpEntity<>("{\"status\":\"HIDDEN\"}",
                        authHeaders(JwtTestTokens.adminToken("admin-s10"))),
                Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            assertThat(reviewRepository.findById(reviewId)).isPresent()
                    .get().extracting(ReviewJpaEntity::getStatus).isEqualTo(ReviewStatus.HIDDEN);
            assertThat(searchRepository.findById(reviewId)).isPresent()
                    .get().extracting(ReviewDocument::getStatus).isEqualTo("HIDDEN");
        });
    }

    @Test
    @DisplayName("S11: PATCH /{id}/moderate - HIDDEN yorum ürün listelemesinden düşer")
    void moderateReview_WhenHidden_ShouldDisappearFromProductListing() {
        String orderId = "order-" + UUID.randomUUID();
        String customerId = "cust-s11";
        seedEligibility(orderId, customerId, "prod-s11");
        String reviewId = postReview(customerId, orderId, "prod-s11", 2, "eh").getBody();

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() ->
                assertThat(restTemplate.getForObject(url("/api/reviews/product/prod-s11"), String.class))
                        .contains(reviewId));

        restTemplate.exchange(url("/api/reviews/" + reviewId + "/moderate"),
                org.springframework.http.HttpMethod.PATCH,
                new HttpEntity<>("{\"status\":\"HIDDEN\"}",
                        authHeaders(JwtTestTokens.adminToken("admin-s11"))),
                Void.class);

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() ->
                assertThat(restTemplate.getForObject(url("/api/reviews/product/prod-s11"), String.class))
                        .doesNotContain(reviewId));
    }

    @Test
    @DisplayName("S12: PATCH /{id}/reply - Mağaza cevabı Postgres ve Elasticsearch'e yazılır")
    void replyToReview_ShouldPersistStoreReplyToBothStores() {
        String orderId = "order-" + UUID.randomUUID();
        String customerId = "cust-s12";
        seedEligibility(orderId, customerId, "prod-s12");
        String reviewId = postReview(customerId, orderId, "prod-s12", 3, "ortalama").getBody();

        await().atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> assertThat(searchRepository.findById(reviewId)).isPresent());

        ResponseEntity<Void> response = restTemplate.exchange(
                url("/api/reviews/" + reviewId + "/reply"),
                org.springframework.http.HttpMethod.PATCH,
                new HttpEntity<>("{\"replyText\":\"Geri bildiriminiz icin tesekkurler\"}",
                        authHeaders(JwtTestTokens.storeToken("store-s12"))),
                Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            assertThat(reviewRepository.findById(reviewId)).isPresent()
                    .get().extracting(ReviewJpaEntity::getStoreReplyText)
                    .isEqualTo("Geri bildiriminiz icin tesekkurler");
            assertThat(searchRepository.findById(reviewId)).isPresent()
                    .get().extracting(ReviewDocument::getStoreReplyText)
                    .isEqualTo("Geri bildiriminiz icin tesekkurler");
        });
    }

    @Test
    @DisplayName("S13: POST /api/internal/reindex - Postgres'teki kayıtları Elasticsearch'e yeniden indeksler")
    void reindex_ShouldRebuildElasticsearchIndexFromPostgres() {
        String orderId = "order-" + UUID.randomUUID();
        String customerId = "cust-s13";
        seedEligibility(orderId, customerId, "prod-s13");
        String reviewId = postReview(customerId, orderId, "prod-s13", 5, "mukemmel").getBody();

        // ES indeksini bilinçli olarak boz: dokümanı sil, sonra reindex ile geri getir.
        searchRepository.deleteById(reviewId);
        await().atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> assertThat(searchRepository.findById(reviewId)).isEmpty());

        ResponseEntity<String> response =
                restTemplate.postForEntity(url("/api/internal/reindex"), null, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("indexed");

        await().atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> assertThat(searchRepository.findById(reviewId)).isPresent());
    }

    @Test
    @DisplayName("S14: GET /api/reviews/product/{id} - Ortalama puan yalnızca ACTIVE yorumlardan hesaplanır")
    void getProductReviews_ShouldComputeAverageFromActiveReviewsOnly() {
        String customerA = "cust-s14a";
        String customerB = "cust-s14b";
        String orderA = "order-" + UUID.randomUUID();
        String orderB = "order-" + UUID.randomUUID();
        seedEligibility(orderA, customerA, "prod-s14");
        seedEligibility(orderB, customerB, "prod-s14");

        postReview(customerA, orderA, "prod-s14", 5, "harika");
        String secondId = postReview(customerB, orderB, "prod-s14", 1, "berbat").getBody();

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() ->
                assertThat(restTemplate.getForObject(url("/api/reviews/product/prod-s14"), String.class))
                        .contains("\"averageRating\":3.0"));

        restTemplate.exchange(url("/api/reviews/" + secondId + "/moderate"),
                org.springframework.http.HttpMethod.PATCH,
                new HttpEntity<>("{\"status\":\"HIDDEN\"}",
                        authHeaders(JwtTestTokens.adminToken("admin-s14"))),
                Void.class);

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() ->
                assertThat(restTemplate.getForObject(url("/api/reviews/product/prod-s14"), String.class))
                        .contains("\"averageRating\":5.0"));
    }

    @Test
    @DisplayName("S15: GET /api/reviews/me - Gerçek filtre zinciriyle JWT sahibinin yorumları döner")
    void getMyReviews_WithRealSecurityChain_ShouldReturnOnlyOwnReviews() {
        String customerId = "cust-s15";
        String orderId = "order-" + UUID.randomUUID();
        seedEligibility(orderId, customerId, "prod-s15");
        String reviewId = postReview(customerId, orderId, "prod-s15", 4, "iyi").getBody();

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            ResponseEntity<String> response = restTemplate.exchange(url("/api/reviews/me"),
                    org.springframework.http.HttpMethod.GET,
                    new HttpEntity<>(authHeaders(JwtTestTokens.customerToken(customerId))),
                    String.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).contains(reviewId);
        });

        ResponseEntity<String> other = restTemplate.exchange(url("/api/reviews/me"),
                org.springframework.http.HttpMethod.GET,
                new HttpEntity<>(authHeaders(JwtTestTokens.customerToken("baska-musteri"))),
                String.class);
        assertThat(other.getBody()).doesNotContain(reviewId);
    }

    @Test
    @DisplayName("S16: Yetkilendirme - CUSTOMER token'ı moderasyon endpoint'inde 403 alır (gerçek zincir)")
    void moderateReview_WhenCustomerToken_ShouldReturn403WithRealFilterChain() {
        ResponseEntity<String> response = restTemplate.exchange(
                url("/api/reviews/any-id/moderate"),
                org.springframework.http.HttpMethod.PATCH,
                new HttpEntity<>("{\"status\":\"HIDDEN\"}",
                        authHeaders(JwtTestTokens.customerToken("cust-s16"))),
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("S17: /error permitAll - İş kuralı ihlali 403 maskesi ile değil gerçek 5xx olarak döner [BUGFIX regresyonu]")
    void businessRuleViolation_ShouldSurfaceAsServerErrorNotMaskedAs403() {
        // SecurityConfig'te "/error" permitAll DEĞİLKEN, handler'dan fırlayan her
        // exception ERROR dispatch'te /error'a giderdi; JwtAuthFilter ERROR
        // dispatch'te çalışmadığı için SecurityContext boş olur ve yanıt 403'e
        // dönüşerek asıl hatayı gizlerdi. Burada yetkili bir CUSTOMER uygun
        // olmayan bir ürüne yorum yazmaya çalışıyor: sonuç 403 DEĞİL 5xx olmalı.
        ResponseEntity<String> response =
                postReview("cust-s17", "order-hic-kargolanmadi", "prod-s17", 5, "olmamali");

        assertThat(response.getStatusCode()).isNotEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getStatusCode().is5xxServerError()).isTrue();
    }
}
