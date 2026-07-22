package com.payment.subsystem;

import com.payment.application.port.in.PaymentCommandUseCase;
import com.payment.application.port.in.PaymentQueryUseCase;
import com.payment.domain.exception.InvalidRefundException;
import com.payment.domain.model.Payment;
import com.payment.domain.model.PaymentStatus;
import com.payment.infrastructure.messaging.RabbitMQConfig;
import com.payment.infrastructure.messaging.publisher.OutboxEventPublisher;
import com.payment.infrastructure.persistence.entity.OutboxJpaEntity;
import com.payment.infrastructure.persistence.entity.PaymentJpaEntity;
import com.payment.infrastructure.persistence.repository.OutboxRepository;
import com.payment.infrastructure.persistence.repository.PaymentRepository;
import com.payment.infrastructure.search.repository.PaymentSearchRepository;
import com.payment.support.PaymentTestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;

/**
 * Katman: SUBSYSTEM - gerçek Postgres + RabbitMQ + Elasticsearch.
 * <p>
 * Beyaz kutu: repository ve use case bean'leri inject edilerek ödemenin yazma
 * modelindeki, okuma modelindeki ve outbox tablosundaki izdüşümü doğrulanır.
 * <p>
 * <b>ÇALIŞTIRMA ÖNKOŞULU: Docker.</b> {@code @EnabledIf} {@code @Inherited} olmadığı
 * için koşul base sınıfta değil, burada (somut sınıfta) tanımlıdır.
 */
@EnabledIf("com.payment.support.DockerAvailability#isDockerAvailable")
@DisplayName("SUBSYSTEM - Ödeme yaşam döngüsü (Postgres + RabbitMQ + Elasticsearch)")
class PaymentLifecycleSubsystemTest extends AbstractPaymentSubsystemTest {

    @Autowired
    private PaymentCommandUseCase commandUseCase;

    @Autowired
    private PaymentQueryUseCase queryUseCase;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private OutboxRepository outboxRepository;

    @Autowired
    private PaymentSearchRepository paymentSearchRepository;

    @Autowired
    private OutboxEventPublisher outboxEventPublisher;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    private UUID orderId;
    private UUID customerId;

    @BeforeEach
    void resetState() {
        outboxRepository.deleteAll();
        paymentRepository.deleteAll();
        paymentSearchRepository.deleteAll();
        orderId = UUID.randomUUID();
        customerId = UUID.randomUUID();
    }

    @Test
    @DisplayName("S1: processOrderApproved - Ödeme PostgreSQL'e COMPLETED yazılır, tutar kuruşuna kadar korunur")
    void processOrderApproved_ShouldPersistCompletedPaymentWithExactAmount() {
        commandUseCase.processOrderApproved(orderId, customerId, new BigDecimal("1234.56"));

        List<PaymentJpaEntity> payments = paymentRepository.findAll();
        assertThat(payments).hasSize(1);
        assertThat(payments.get(0).getStatus()).isEqualTo(PaymentStatus.COMPLETED);
        assertThat(payments.get(0).getAmount()).isEqualByComparingTo(new BigDecimal("1234.56"));
    }

    @Test
    @DisplayName("S2: processOrderApproved - Aynı sipariş için ikinci kez çağrılırsa İKİNCİ tahsilat yapılmaz (idempotency)")
    void processOrderApproved_WhenCalledTwiceForSameOrder_ShouldCreateOnlyOnePayment() {
        commandUseCase.processOrderApproved(orderId, customerId, new BigDecimal("100.00"));
        commandUseCase.processOrderApproved(orderId, customerId, new BigDecimal("100.00"));

        assertThat(paymentRepository.findAll())
                .as("tekrar teslim edilen order.approved olayı ikinci ödeme kaydı oluşturmamalı")
                .hasSize(1);
        assertThat(outboxRepository.findAll())
                .as("ikinci kez PaymentCompletedEvent yayınlanmamalı")
                .hasSize(1);
    }

    @Test
    @DisplayName("S3: processOrderApproved - Okuma modeli (Elasticsearch) ödemeyi görünür kılar")
    void processOrderApproved_ShouldSynchronizeReadModelInElasticsearch() {
        commandUseCase.processOrderApproved(orderId, customerId, new BigDecimal("55.55"));

        await().atMost(Duration.ofSeconds(20)).untilAsserted(() -> {
            List<Payment> found = queryUseCase.getPaymentsByCustomerId(customerId);
            assertThat(found).hasSize(1);
            assertThat(found.get(0).getAmount()).isEqualByComparingTo(new BigDecimal("55.55"));
            assertThat(found.get(0).getStatus()).isEqualTo(PaymentStatus.COMPLETED);
        });
    }

    @Test
    @DisplayName("S4: processOrderApproved - Limit üstü tutar FAILED kaydedilir ve PaymentFailedEvent yazılır")
    void processOrderApproved_WhenAmountExceedsGatewayLimit_ShouldPersistFailed() {
        commandUseCase.processOrderApproved(orderId, customerId, new BigDecimal("99999.00"));

        assertThat(paymentRepository.findAll().get(0).getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(outboxRepository.findAll()).extracting(OutboxJpaEntity::getType)
                .containsExactly("PaymentFailedEvent");
    }

    @Test
    @DisplayName("S5: İade akışı - COMPLETED -> REFUND_REQUESTED -> REFUNDED durum geçişleri kalıcılaşır")
    void refundFlow_ShouldPersistEveryStatusTransition() {
        commandUseCase.processOrderApproved(orderId, customerId, new BigDecimal("300.00"));
        UUID paymentId = paymentRepository.findAll().get(0).getId();

        commandUseCase.requestRefund(paymentId, customerId, "beğenmedim");
        assertThat(paymentRepository.findById(paymentId).orElseThrow().getStatus())
                .isEqualTo(PaymentStatus.REFUND_REQUESTED);

        commandUseCase.approveRefund(paymentId);
        assertThat(paymentRepository.findById(paymentId).orElseThrow().getStatus())
                .isEqualTo(PaymentStatus.REFUNDED);
        assertThat(paymentRepository.findById(paymentId).orElseThrow().getAmount())
                .as("iade tutarı ödenen tutarla birebir aynı kalmalı")
                .isEqualByComparingTo(new BigDecimal("300.00"));
    }

    @Test
    @DisplayName("S6: İade akışı - Aynı ödeme ikinci kez iade edilemez (çift iade veritabanına yansımaz)")
    void refundFlow_WhenApprovedTwice_ShouldRejectSecondApproval() {
        commandUseCase.processOrderApproved(orderId, customerId, new BigDecimal("300.00"));
        UUID paymentId = paymentRepository.findAll().get(0).getId();
        commandUseCase.requestRefund(paymentId, customerId, "beğenmedim");
        commandUseCase.approveRefund(paymentId);

        assertThatThrownBy(() -> commandUseCase.approveRefund(paymentId))
                .isInstanceOf(InvalidRefundException.class);

        assertThat(paymentRepository.findById(paymentId).orElseThrow().getStatus())
                .isEqualTo(PaymentStatus.REFUNDED);
        assertThat(outboxRepository.findAll()).extracting(OutboxJpaEntity::getType)
                .filteredOn("PaymentRefundedEvent"::equals)
                .hasSize(1);
    }

    @Test
    @DisplayName("S7: İade akışı - Başka müşteri iade talebinde bulunamaz, durum değişmez (IDOR)")
    void refundFlow_WhenAnotherCustomerRequestsRefund_ShouldRejectAndKeepStatus() {
        commandUseCase.processOrderApproved(orderId, customerId, new BigDecimal("300.00"));
        UUID paymentId = paymentRepository.findAll().get(0).getId();

        assertThatThrownBy(() -> commandUseCase.requestRefund(paymentId, UUID.randomUUID(), "benim değil"))
                .isInstanceOf(InvalidRefundException.class);

        assertThat(paymentRepository.findById(paymentId).orElseThrow().getStatus())
                .isEqualTo(PaymentStatus.COMPLETED);
    }

    /**
     * REGRESYON: @EnableScheduling eksikti ve payment.exchange hiç tanımlı değildi.
     * Bu test yayınlayıcıyı elle tetikleyip outbox satırının GERÇEKTEN broker'a
     * gittiğini ve tablodan silindiğini doğrular.
     */
    @Test
    @DisplayName("S8: Outbox - Yayınlanan olay RabbitMQ'ya gider ve outbox satırı silinir")
    void outboxPublisher_ShouldPublishToRabbitAndClearTheRow() {
        commandUseCase.processOrderApproved(orderId, customerId, new BigDecimal("10.00"));
        assertThat(outboxRepository.findAll()).hasSize(1);

        String queueName = rabbitTemplate.execute(channel ->
                channel.queueDeclare().getQueue());
        rabbitTemplate.execute(channel -> {
            channel.queueBind(queueName, RabbitMQConfig.PAYMENT_EXCHANGE, "payment.completed");
            return null;
        });

        // Kuyruk bağlandıktan SONRA üretilen olay yayınlanmalı.
        UUID secondOrderId = UUID.randomUUID();
        commandUseCase.processOrderApproved(secondOrderId, customerId, new BigDecimal("20.00"));
        outboxEventPublisher.publishOutboxEvents();

        assertThat(outboxRepository.findAll())
                .as("yayınlanan satırlar outbox'tan temizlenmeli")
                .isEmpty();

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() ->
                assertThat(rabbitTemplate.receive(queueName, 2000))
                        .as("payment.exchange tanımlı değilse mesaj sessizce düşer")
                        .isNotNull());
    }

    @Test
    @DisplayName("S9: order.approved kuyruğu - Broker'a düşen olay ödeme oluşturur (uçtan uca listener)")
    void orderApprovedListener_WhenEventArrivesOnQueue_ShouldCreatePayment() {
        rabbitTemplate.convertAndSend(RabbitMQConfig.ORDER_EXCHANGE, RabbitMQConfig.ORDER_APPROVED_ROUTING_KEY,
                PaymentTestFixtures.orderApprovedPayload(orderId, customerId, "77.70"));

        await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> {
            List<PaymentJpaEntity> payments = paymentRepository.findAll();
            assertThat(payments).hasSize(1);
            assertThat(payments.get(0).getOrderId()).isEqualTo(orderId);
            assertThat(payments.get(0).getAmount()).isEqualByComparingTo(new BigDecimal("77.70"));
        });
    }

    @Test
    @DisplayName("S10: order.approved kuyruğu - Aynı olay iki kez teslim edilirse yalnızca BİR ödeme oluşur (idempotency)")
    void orderApprovedListener_WhenSameEventDeliveredTwice_ShouldCreateOnlyOnePayment() {
        String payload = PaymentTestFixtures.orderApprovedPayload(orderId, customerId, "50.00");

        rabbitTemplate.convertAndSend(RabbitMQConfig.ORDER_EXCHANGE, RabbitMQConfig.ORDER_APPROVED_ROUTING_KEY, payload);
        await().atMost(Duration.ofSeconds(15))
                .untilAsserted(() -> assertThat(paymentRepository.findAll()).hasSize(1));

        rabbitTemplate.convertAndSend(RabbitMQConfig.ORDER_EXCHANGE, RabbitMQConfig.ORDER_APPROVED_ROUTING_KEY, payload);

        // İkinci teslimat işlendikten sonra da tek ödeme kalmalı.
        await().during(Duration.ofSeconds(3)).atMost(Duration.ofSeconds(15))
                .untilAsserted(() -> assertThat(paymentRepository.findAll())
                        .as("müşteriden iki kez para çekilmemeli")
                        .hasSize(1));
    }
}
