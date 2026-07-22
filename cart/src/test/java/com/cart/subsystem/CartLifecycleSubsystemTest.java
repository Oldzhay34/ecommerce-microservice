package com.cart.subsystem;

import com.cart.api.dto.AddToCartRequest;
import com.cart.api.dto.CartResponse;
import com.cart.api.dto.UpdateCartItemRequest;
import com.cart.application.port.in.CartCommandUseCase;
import com.cart.application.port.in.CartQueryUseCase;
import com.cart.application.port.out.CartCachePort;
import com.cart.domain.model.Cart;
import com.cart.infrastructure.messaging.publisher.OutboxEventPublisher;
import com.cart.infrastructure.persistence.entity.CartJpaEntity;
import com.cart.infrastructure.persistence.entity.OutboxEventJpaEntity;
import com.cart.infrastructure.persistence.repository.CartRepository;
import com.cart.infrastructure.persistence.repository.OutboxEventRepository;
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

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Katman: SUBSYSTEM (gerçek Postgres + RabbitMQ + Redis).
 *
 * Buradaki testler black-box DEĞİLDİR: bean'ler inject edilerek veritabanı,
 * cache ve outbox durumu doğrudan doğrulanır. Amaç, use case ile altyapı
 * adaptörleri arasındaki entegrasyonun (JPA eşlemeleri, lazy koleksiyon,
 * cache invalidation, outbox yazımı ve yayını) gerçekten çalıştığını görmek.
 */
@DisplayName("SUBSYSTEM - Cart yaşam döngüsü (Postgres + RabbitMQ + Redis)")
class CartLifecycleSubsystemTest extends AbstractCartSubsystemTest {

    private static final String VERIFICATION_QUEUE = "test.verification.cart.events.q";

    @Autowired
    private CartCommandUseCase cartCommandUseCase;

    @Autowired
    private CartQueryUseCase cartQueryUseCase;

    @Autowired
    private CartCachePort cartCachePort;

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    // Scheduler test'te kapalı; publisher elle tetiklenir.
    @Autowired
    private OutboxEventPublisher outboxEventPublisher;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private AmqpAdmin amqpAdmin;

    private UUID userId;
    private UUID productA;
    private UUID productB;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        productA = UUID.randomUUID();
        productB = UUID.randomUUID();

        outboxEventRepository.deleteAll();

        Queue queue = QueueBuilder.durable(VERIFICATION_QUEUE).build();
        TopicExchange exchange = new TopicExchange("cart.exchange");
        // '#' tüm cart.* routing key'lerini yakalar.
        Binding binding = BindingBuilder.bind(queue).to(exchange).with("cart.#");

        amqpAdmin.declareQueue(queue);
        amqpAdmin.declareExchange(exchange);
        amqpAdmin.declareBinding(binding);
        amqpAdmin.purgeQueue(VERIFICATION_QUEUE, true);
    }

    private AddToCartRequest addRequest(UUID productId, int quantity, String price) {
        AddToCartRequest request = new AddToCartRequest();
        request.setProductId(productId);
        request.setQuantity(quantity);
        request.setPrice(new BigDecimal(price));
        return request;
    }

    @Test
    @DisplayName("S1: addItemToCart - Sepet ve satırlar Postgres'e yazılır, toplam tutar kalıcı olur")
    void addItemToCart_ShouldPersistCartAndItemsInPostgres() {
        cartCommandUseCase.addItemToCart(userId, addRequest(productA, 3, "10.00"));

        Optional<CartJpaEntity> persisted = cartRepository.findByUserId(userId);

        assertThat(persisted).isPresent();
        assertThat(persisted.get().getTotalAmount()).isEqualByComparingTo("30.00");
        assertThat(persisted.get().getItems()).hasSize(1);
        assertThat(persisted.get().getItems().get(0).getProductId()).isEqualTo(productA);
        assertThat(persisted.get().getItems().get(0).getQuantity()).isEqualTo(3);
    }

    @Test
    @DisplayName("S2: findByUserId - Satır koleksiyonu transaction dışında da okunabilir (LazyInitializationException yok)")
    void findByUserId_ShouldFetchItemsEagerlyOutsideTransaction() {
        cartCommandUseCase.addItemToCart(userId, addRequest(productA, 2, "5.00"));

        // Bu çağrı açık bir transaction'ın DIŞINDA yapılır. CartRepository
        // üzerindeki @EntityGraph olmasaydı burada LazyInitializationException
        // alınırdı.
        CartJpaEntity entity = cartRepository.findByUserId(userId).orElseThrow();

        assertThat(entity.getItems()).hasSize(1);
        assertThat(entity.getItems().get(0).getPrice()).isEqualByComparingTo("5.00");
    }

    @Test
    @DisplayName("S3: addItemToCart - Aynı ürün tekrar eklenince veritabanında yeni satır açılmaz, miktar birleşir")
    void addItemToCart_WhenSameProductAddedTwice_ShouldMergeQuantityInDatabase() {
        cartCommandUseCase.addItemToCart(userId, addRequest(productA, 2, "10.00"));
        cartCommandUseCase.addItemToCart(userId, addRequest(productA, 3, "10.00"));

        CartJpaEntity entity = cartRepository.findByUserId(userId).orElseThrow();

        assertThat(entity.getItems()).hasSize(1);
        assertThat(entity.getItems().get(0).getQuantity()).isEqualTo(5);
        assertThat(entity.getTotalAmount()).isEqualByComparingTo("50.00");
    }

    @Test
    @DisplayName("S4: updateCartItemQuantity - Miktar 0 verilince satır veritabanından da silinir (orphanRemoval)")
    void updateCartItemQuantity_WhenQuantityZero_ShouldDeleteRowFromDatabase() {
        cartCommandUseCase.addItemToCart(userId, addRequest(productA, 2, "10.00"));
        cartCommandUseCase.addItemToCart(userId, addRequest(productB, 1, "7.00"));

        cartCommandUseCase.updateCartItemQuantity(userId, productA, new UpdateCartItemRequest(0));

        CartJpaEntity entity = cartRepository.findByUserId(userId).orElseThrow();
        assertThat(entity.getItems()).hasSize(1);
        assertThat(entity.getItems().get(0).getProductId()).isEqualTo(productB);
        assertThat(entity.getTotalAmount()).isEqualByComparingTo("7.00");
    }

    @Test
    @DisplayName("S5: getCart - İlk okuma Redis'i ısıtır, yazma komutu cache'i invalidate eder (HIT/MISS döngüsü)")
    void getCart_ShouldWarmRedisCacheAndWriteCommandShouldInvalidateIt() {
        cartCommandUseCase.addItemToCart(userId, addRequest(productA, 2, "10.00"));

        // Yazma sonrası cache invalidate edilmiş olmalı (MISS).
        assertThat(cartCachePort.getCartByUserId(userId)).isEmpty();

        // İlk okuma cache'i ısıtır.
        CartResponse first = cartQueryUseCase.getCart(userId);
        assertThat(first.getTotalAmount()).isEqualByComparingTo("20.00");

        Optional<Cart> cached = cartCachePort.getCartByUserId(userId);
        assertThat(cached).isPresent();
        assertThat(cached.get().getUserId()).isEqualTo(userId);
        assertThat(cached.get().getTotalAmount()).isEqualByComparingTo("20.00");

        // Yeni bir yazma cache'i tekrar düşürür.
        cartCommandUseCase.addItemToCart(userId, addRequest(productB, 1, "5.00"));
        assertThat(cartCachePort.getCartByUserId(userId)).isEmpty();
    }

    @Test
    @DisplayName("S6: addItemToCart - Outbox'a CartUpdatedEvent yazılır ve payload gerçekten serileştirilebilir")
    void addItemToCart_ShouldWriteSerializableOutboxEvent() {
        cartCommandUseCase.addItemToCart(userId, addRequest(productA, 2, "10.00"));

        List<OutboxEventJpaEntity> pending = outboxEventRepository.findByProcessedFalse();

        assertThat(pending).hasSize(1);
        assertThat(pending.get(0).getEventType()).isEqualTo("CartUpdatedEvent");
        assertThat(pending.get(0).getAggregateId()).isEqualTo(userId.toString());
        assertThat(pending.get(0).getPayload()).contains(productA.toString());
        assertThat(pending.get(0).getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("S7: publishEvents - Bekleyen outbox kaydı RabbitMQ'ya yayınlanır ve processed=true olur")
    void publishEvents_ShouldDeliverOutboxEventToRabbitAndMarkProcessed() {
        cartCommandUseCase.addItemToCart(userId, addRequest(productA, 2, "10.00"));

        outboxEventPublisher.publishEvents();

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            Message message = rabbitTemplate.receive(VERIFICATION_QUEUE);
            assertThat(message).isNotNull();
            assertThat(new String(message.getBody(), StandardCharsets.UTF_8))
                    .contains(userId.toString())
                    .contains(productA.toString());
        });

        assertThat(outboxEventRepository.findByProcessedFalse()).isEmpty();
    }

    @Test
    @DisplayName("S8: clearCart - Sepet boşaltılır, toplam sıfırlanır ve CartClearedEvent outbox'a düşer")
    void clearCart_ShouldEmptyCartInDatabaseAndWriteClearedEvent() {
        cartCommandUseCase.addItemToCart(userId, addRequest(productA, 2, "10.00"));
        outboxEventRepository.deleteAll();

        cartCommandUseCase.clearCart(userId);

        CartJpaEntity entity = cartRepository.findByUserId(userId).orElseThrow();
        assertThat(entity.getItems()).isEmpty();
        assertThat(entity.getTotalAmount()).isEqualByComparingTo("0");

        assertThat(outboxEventRepository.findByProcessedFalse())
                .extracting(OutboxEventJpaEntity::getEventType)
                .containsExactly("CartClearedEvent");
    }

    @Test
    @DisplayName("S9: clearCart - Hiç sepeti olmayan kullanıcı için hiçbir kayıt ve event üretilmez")
    void clearCart_WhenUserHasNoCart_ShouldNotCreateAnythingelse() {
        UUID unknownUser = UUID.randomUUID();

        cartCommandUseCase.clearCart(unknownUser);

        assertThat(cartRepository.findByUserId(unknownUser)).isEmpty();
        assertThat(outboxEventRepository.findByProcessedFalse())
                .allSatisfy(event -> assertThat(event.getAggregateId()).isNotEqualTo(unknownUser.toString()));
    }
}
