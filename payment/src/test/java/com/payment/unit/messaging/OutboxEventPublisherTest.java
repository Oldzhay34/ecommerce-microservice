package com.payment.unit.messaging;

import com.payment.infrastructure.messaging.publisher.OutboxEventPublisher;
import com.payment.infrastructure.persistence.entity.OutboxJpaEntity;
import com.payment.infrastructure.persistence.repository.OutboxRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Katman: UNIT - outbox yayınlayıcısı.
 * Ödeme olayları sipariş servisinin siparişi kapatması için hayatidir; yayın
 * başarısız olduğunda satırın SİLİNMEMESİ (yeniden denenebilmesi) kritiktir.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UNIT - OutboxEventPublisher (ödeme olaylarının yayınlanması)")
class OutboxEventPublisherTest {

    @Mock
    private OutboxRepository outboxRepository;

    @Mock
    private RabbitTemplate rabbitTemplate;

    private OutboxEventPublisher publisher;

    @BeforeEach
    void setUp() {
        publisher = new OutboxEventPublisher(outboxRepository, rabbitTemplate);
    }

    private static OutboxJpaEntity event(String type, String payload) {
        return new OutboxJpaEntity("Payment", UUID.randomUUID().toString(), type, payload);
    }

    @ParameterizedTest(name = "{0} -> {1}")
    @CsvSource({
            "PaymentCompletedEvent, payment.completed",
            "PaymentFailedEvent,    payment.failed",
            "PaymentRefundedEvent,  payment.refunded",
            "RefundRequestedEvent,  payment.unknown",
            "SomethingElse,         payment.unknown"
    })
    @DisplayName("U69: publishOutboxEvents - Olay tipi doğru routing key'e eşlenir")
    void publishOutboxEvents_ShouldMapEventTypeToRoutingKey(String type, String expectedRoutingKey) {
        when(outboxRepository.findAll()).thenReturn(List.of(event(type, "{\"a\":1}")));

        publisher.publishOutboxEvents();

        verify(rabbitTemplate).convertAndSend(eq("payment.exchange"), eq(expectedRoutingKey), eq("{\"a\":1}"));
    }

    @Test
    @DisplayName("U70: publishOutboxEvents - Yayınlanan satır yayından SONRA silinir")
    void publishOutboxEvents_ShouldDeleteRowAfterPublishing() {
        OutboxJpaEntity entity = event("PaymentCompletedEvent", "{}");
        when(outboxRepository.findAll()).thenReturn(List.of(entity));

        publisher.publishOutboxEvents();

        InOrder order = inOrder(rabbitTemplate, outboxRepository);
        order.verify(rabbitTemplate).convertAndSend(any(String.class), any(String.class), any(Object.class));
        order.verify(outboxRepository).delete(entity);
    }

    @Test
    @DisplayName("U71: publishOutboxEvents - Yayın başarısız olursa satır SİLİNMEZ (olay kaybolmaz)")
    void publishOutboxEvents_WhenBrokerFails_ShouldNotDeleteRow() {
        OutboxJpaEntity entity = event("PaymentCompletedEvent", "{}");
        when(outboxRepository.findAll()).thenReturn(List.of(entity));
        doThrow(new RuntimeException("broker down"))
                .when(rabbitTemplate).convertAndSend(any(String.class), any(String.class), any(Object.class));

        assertThatThrownBy(() -> publisher.publishOutboxEvents()).isInstanceOf(RuntimeException.class);

        verify(outboxRepository, never()).delete(any());
    }

    @Test
    @DisplayName("U72: publishOutboxEvents - Outbox boşsa broker'a hiç dokunulmaz")
    void publishOutboxEvents_WhenOutboxIsEmpty_ShouldNotTouchBroker() {
        when(outboxRepository.findAll()).thenReturn(List.of());

        publisher.publishOutboxEvents();

        verifyNoInteractions(rabbitTemplate);
    }

    @Test
    @DisplayName("U73: publishOutboxEvents - Birden çok olay tek turda yayınlanır ve hepsi silinir")
    void publishOutboxEvents_WhenMultipleEvents_ShouldPublishAndDeleteEachOne() {
        OutboxJpaEntity first = event("PaymentCompletedEvent", "{\"i\":1}");
        OutboxJpaEntity second = event("PaymentRefundedEvent", "{\"i\":2}");
        when(outboxRepository.findAll()).thenReturn(List.of(first, second));

        publisher.publishOutboxEvents();

        verify(rabbitTemplate).convertAndSend("payment.exchange", "payment.completed", "{\"i\":1}");
        verify(rabbitTemplate).convertAndSend("payment.exchange", "payment.refunded", "{\"i\":2}");
        verify(outboxRepository).delete(first);
        verify(outboxRepository).delete(second);
    }
}
