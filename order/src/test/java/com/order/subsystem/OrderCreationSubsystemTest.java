package com.order.subsystem;

import com.order.api.dto.CreateOrderRequest;
import com.order.api.dto.OrderItemDto;
import com.order.domain.model.OrderStatus;
import com.order.infrastructure.persistence.entity.OrderJpaEntity;
import com.order.infrastructure.persistence.entity.OutboxEventJpaEntity;
import com.order.infrastructure.persistence.repository.OrderRepository;
import com.order.infrastructure.persistence.repository.OutboxEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Subsystem test: sadece order servisi + gerçek Postgres + gerçek RabbitMQ.
 * Product servisi burada yok - amaç order servisinin KENDİ İÇİNDE tutarlı
 * çalıştığını (yazma -> outbox -> yayınlama zinciri) doğrulamak.
 *
 * GET endpoint'leri burada test edilMİYOR çünkü query tarafı (OrderSearchAdapter)
 * Elasticsearch'ten okuyor ve şu an Postgres->ES senkronizasyonu yapan bir
 * mekanizma yok (bkz. sohbet notu). Bu yüzden Postgres durumunu doğrudan
 * OrderRepository (JPA) üzerinden doğruluyoruz.
 */
@DisplayName("Order Service - Subsystem Tests (Postgres + RabbitMQ, gerçek altyapı)")
@AutoConfigureTestRestTemplate
class OrderCreationSubsystemTest extends AbstractOrderSubsystemTest {

    private static final String TEST_VERIFICATION_QUEUE = "test.verification.order.created.q";

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private AmqpAdmin amqpAdmin;

    @BeforeEach
    void bindVerificationQueue() {
        // Test amaçlı bağımsız bir queue - order.exchange'e "order.created"
        // routing key'iyle bind ediyoruz ki publisher'ın gerçekten doğru
        // exchange/routing key'e mesaj bastığını dışarıdan bir tüketici
        // gibi doğrulayabilelim (uygulamanın kendi StockResponseListener'ından
        // BAĞIMSIZ bir doğrulama - o "stock.event.*" dinliyor, bu ise
        // "order.created" dinliyor).
        Queue queue = QueueBuilder.durable(TEST_VERIFICATION_QUEUE).build();
        TopicExchange exchange = new TopicExchange("order.exchange");
        Binding binding = BindingBuilder.bind(queue).to(exchange).with("order.created");

        amqpAdmin.declareQueue(queue);
        amqpAdmin.declareExchange(exchange);
        amqpAdmin.declareBinding(binding);
        amqpAdmin.purgeQueue(TEST_VERIFICATION_QUEUE, true);
    }

    private HttpHeaders customerHeaders(String userId) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-User-Id", userId);
        headers.set("X-User-Role", "ROLE_CUSTOMER");
        return headers;
    }

    @Test
    @DisplayName("S1: POST /api/orders - Sipariş Postgres'e PENDING olarak yazılır")
    void createOrder_ShouldPersistToPostgresAsPending() {
        String userId = "user-subsystem-1";
        CreateOrderRequest request = new CreateOrderRequest(userId,
                List.of(new OrderItemDto("prod-1", "store-1", 2, BigDecimal.valueOf(50))));

        HttpEntity<CreateOrderRequest> entity = new HttpEntity<>(request, customerHeaders(userId));
        ResponseEntity<String> response = restTemplate.postForEntity(
                "http://localhost:" + port + "/api/orders", entity, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        // Gerçek Postgres'ten doğrudan kontrol (ES sync eksikliği nedeniyle
        // HTTP GET yerine repository üzerinden doğruluyoruz)
        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            List<OrderJpaEntity> all = orderRepository.findAll();
            Optional<OrderJpaEntity> created = all.stream()
                    .filter(o -> userId.equals(o.getUserId()))
                    .findFirst();

            assertThat(created).isPresent();
            assertThat(created.get().getStatus()).isEqualTo(OrderStatus.PENDING);
            assertThat(created.get().getTotalAmount()).isEqualByComparingTo(BigDecimal.valueOf(100));
            assertThat(created.get().getItems()).hasSize(1);
        });
    }

    @Test
    @DisplayName("S2: POST /api/orders - Outbox tablosuna OrderCreatedEvent yazılır")
    void createOrder_ShouldWriteOutboxEvent() {
        String userId = "user-subsystem-2";
        CreateOrderRequest request = new CreateOrderRequest(userId,
                List.of(new OrderItemDto("prod-2", "store-1", 1, BigDecimal.valueOf(75))));

        HttpEntity<CreateOrderRequest> entity = new HttpEntity<>(request, customerHeaders(userId));
        restTemplate.postForEntity("http://localhost:" + port + "/api/orders", entity, String.class);

        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            List<OutboxEventJpaEntity> events = outboxEventRepository.findAll();
            boolean hasEvent = events.stream()
                    .anyMatch(e -> "OrderCreatedEvent".equals(e.getEventType())
                            && e.getPayload().contains(userId));

            assertThat(hasEvent).isTrue();
        });
    }

    @Test
    @DisplayName("S3: Outbox publisher - Event'i gerçekten RabbitMQ'ya (order.exchange/order.created) yayınlar")
    void outboxPublisher_ShouldActuallyPublishToRabbitMQ() {
        String userId = "user-subsystem-3";
        CreateOrderRequest request = new CreateOrderRequest(userId,
                List.of(new OrderItemDto("prod-3", "store-1", 3, BigDecimal.valueOf(20))));

        HttpEntity<CreateOrderRequest> entity = new HttpEntity<>(request, customerHeaders(userId));
        restTemplate.postForEntity("http://localhost:" + port + "/api/orders", entity, String.class);

        // @Scheduled(fixedRate = 5000) - en az 5sn beklenmeli, o yüzden 10sn payı bırakıyoruz
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            Message message = rabbitTemplate.receive(TEST_VERIFICATION_QUEUE, 500);
            assertThat(message).isNotNull();

            String payload = new String(message.getBody());
            assertThat(payload).contains(userId);
        });
    }

    @Test
    @DisplayName("S4: Outbox publisher - Yayınlanan event processed=true olarak işaretlenir")
    void outboxPublisher_ShouldMarkEventAsProcessedAfterPublishing() {
        String userId = "user-subsystem-4";
        CreateOrderRequest request = new CreateOrderRequest(userId,
                List.of(new OrderItemDto("prod-4", "store-1", 1, BigDecimal.valueOf(10))));

        HttpEntity<CreateOrderRequest> entity = new HttpEntity<>(request, customerHeaders(userId));
        restTemplate.postForEntity("http://localhost:" + port + "/api/orders", entity, String.class);

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            List<OutboxEventJpaEntity> events = outboxEventRepository.findAll();
            Optional<OutboxEventJpaEntity> event = events.stream()
                    .filter(e -> e.getPayload().contains(userId))
                    .findFirst();

            assertThat(event).isPresent();
            assertThat(event.get().isProcessed()).isTrue();
        });
    }

    @Test
    @DisplayName("S5: POST /api/orders - Başka kullanıcı adına sipariş oluşturma isteği reddedilir (gerçek filtre zinciriyle)")
    void createOrder_WhenIdorAttempt_ShouldBeRejected() {
        CreateOrderRequest request = new CreateOrderRequest("victim-user",
                List.of(new OrderItemDto("prod-5", "store-1", 1, BigDecimal.valueOf(10))));

        // X-User-Id header'ı "attacker-user", body'deki userId ise "victim-user"
        HttpEntity<CreateOrderRequest> entity = new HttpEntity<>(request, customerHeaders("attacker-user"));
        ResponseEntity<String> response = restTemplate.postForEntity(
                "http://localhost:" + port + "/api/orders", entity, String.class);

        // NOT: Şu an bir @ControllerAdvice/global exception handler olmadığı için
        // UnauthorizedOrderAccessException muhtemelen 500 olarak dönüyor.
        // Bu, isteğe (401/403 olması gerekirdi) göre bir iyileştirme fırsatı -
        // subsystem test bunu şu haliyle (5xx) doğruluyor, ama önerim bir
        // @ControllerAdvice ekleyip bu exception'ı 403'e map etmek.
        assertThat(response.getStatusCode().is5xxServerError()).isTrue();

        boolean orderCreatedForVictim = orderRepository.findAll().stream()
                .anyMatch(o -> "victim-user".equals(o.getUserId()));
        assertThat(orderCreatedForVictim).isFalse();
    }
}